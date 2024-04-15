/*
 *  OpenSlide, a library for reading whole slide image files
 *
 *  Copyright (c) 2024 Benjamin Gilbert
 *  All rights reserved.
 *
 *  OpenSlide is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation, version 2.1.
 *
 *  OpenSlide is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with OpenSlide. If not, see
 *  <http://www.gnu.org/licenses/>.
 *
 */

package org.openslide;

import java.lang.foreign.*;
import static java.lang.foreign.ValueLayout.*;
import java.lang.invoke.*;
import java.lang.ref.Cleaner;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

// restricted method calls are expected
// we take locks with try-with-resources that aren't accessed inside the block
@SuppressWarnings({"restricted", "try"})
class OpenSlideFFM {
    private static final Arena LIBRARY_ARENA = Arena.ofAuto();

    private static final SymbolLookup SYMBOL_LOOKUP = getSymbolLookup();

    private static final AddressLayout C_POINTER = ADDRESS.withTargetLayout(
            MemoryLayout.sequenceLayout(Long.MAX_VALUE, JAVA_BYTE));

    private static final MemoryLayout SIZE_T = Linker.nativeLinker()
            .canonicalLayouts().get("size_t");

    private OpenSlideFFM() {
    }

    private static SymbolLookup getSymbolLookup() {
        // return upon first success, rather than chaining with
        // SymbolLookup.or(), since the latter would continue dlopen()ing
        // even after we've found the library
        SymbolLookup lookup = SymbolLookup.loaderLookup();
        if (lookup.find("openslide_open").isPresent()) {
            return lookup;
        }
        String tmpl = System.mapLibraryName("openslide");
        String lname;
        if (tmpl.endsWith(".dll")) {
            lname = "libopenslide-1.dll";
        } else if (tmpl.endsWith(".dylib")) {
            lname = "libopenslide.1.dylib";
        } else {
            lname = tmpl + ".1";
        }
        try {
            return SymbolLookup.libraryLookup(lname, LIBRARY_ARENA);
        } catch (IllegalArgumentException ex) {
            throw new UnsatisfiedLinkError(
                    "Couldn't locate OpenSlide library; add it to the " +
                    "operating system's library search path or use " +
                    "System.load() or System.loadLibrary()");
        }
    }

    private static MethodHandle function(MemoryLayout ret, String name,
            MemoryLayout... args) {
        MemorySegment symbol = SYMBOL_LOOKUP.find(name)
                .orElseThrow(() -> new UnsatisfiedLinkError(
                "Unresolved symbol " + name + "; need OpenSlide >= 4.0.0"));
        FunctionDescriptor desc;
        if (ret != null) {
            desc = FunctionDescriptor.of(ret, args);
        } else {
            desc = FunctionDescriptor.ofVoid(args);
        }
        return Linker.nativeLinker().downcallHandle(symbol, desc);
    }

    private static abstract class Ref {
        static abstract class Wrapper implements Runnable {
            private final MemorySegment segment;

            Wrapper(MemorySegment segment) {
                this.segment = segment;
            }

            MemorySegment getSegment() {
                return segment;
            }
        }

        static class ScopedLock implements AutoCloseable {
            private final Lock lock;

            ScopedLock(Lock lock) {
                this.lock = lock;
            }

            void lock() {
                lock.lock();
            }

            @Override
            public void close() {
                lock.unlock();
            }
        }

        private static final Cleaner cleaner = Cleaner.create();

        private Wrapper wrapper;

        private final Cleaner.Cleanable cleanable;

        private final ReentrantReadWriteLock lock =
                new ReentrantReadWriteLock();

        private final ScopedLock readLock = new ScopedLock(lock.readLock());

        private final ScopedLock writeLock = new ScopedLock(lock.writeLock());

        Ref(Wrapper wrapper) {
            this.wrapper = wrapper;
            cleanable = cleaner.register(this, wrapper);
        }

        ScopedLock lock() {
            readLock.lock();
            return readLock;
        }

        private ScopedLock writeLock() {
            writeLock.lock();
            return writeLock;
        }

        MemorySegment getSegment() {
            if (lock.getReadHoldCount() == 0) {
                throw new IllegalStateException("Reference lock not held");
            }
            if (wrapper == null) {
                throw new OpenSlideDisposedException(
                        this.getClass().getSimpleName());
            }
            return wrapper.getSegment();
        }

        void close() {
            try (ScopedLock l = writeLock()) {
                cleanable.clean();
                wrapper = null;
            }
        }
    }

    static class OpenSlideRef extends Ref {
        private static class Wrapper extends Ref.Wrapper {
            private static final MethodHandle close = function(
                    null, "openslide_close", C_POINTER);

            Wrapper(MemorySegment segment) {
                super(segment);
            }

            @Override
            public void run() {
                try {
                    close.invokeExact(getSegment());
                } catch (Throwable ex) {
                    throw wrapException(ex);
                }
            }
        }

        OpenSlideRef(MemorySegment segment) {
            super(new Wrapper(segment));
        }
    }

    static class OpenSlideCacheRef extends Ref {
        private static class Wrapper extends Ref.Wrapper {
            private static final MethodHandle cache_release = function(
                    null, "openslide_cache_release", C_POINTER);

            Wrapper(MemorySegment segment) {
                super(segment);
            }

            @Override
            public void run() {
                try {
                    cache_release.invokeExact(getSegment());
                } catch (Throwable ex) {
                    throw wrapException(ex);
                }
            }
        }

        OpenSlideCacheRef(MemorySegment segment) {
            super(new Wrapper(segment));
        }
    }

    private static String[] segment_to_string_array(MemorySegment seg) {
        int length = 0;
        while (!seg.getAtIndex(C_POINTER, length).equals(MemorySegment.NULL)) {
            length++;
        }
        String[] ret = new String[length];
        for (int i = 0; i < length; i++) {
            ret[i] = seg.getAtIndex(C_POINTER, i).getString(0);
        }
        return ret;
    }

    private static RuntimeException wrapException(Throwable ex) {
        if (ex instanceof RuntimeException) {
            return (RuntimeException) ex;
        }
        return new IllegalArgumentException("Invalid call", ex);
    }

    private static final MethodHandle detect_vendor = function(
            C_POINTER, "openslide_detect_vendor", C_POINTER);

    static String openslide_detect_vendor(String filename) {
        if (filename == null) {
            return null;
        }
        MemorySegment ret;
        try (Arena arena = Arena.ofConfined()) {
            ret = (MemorySegment) detect_vendor.invokeExact(
                    arena.allocateFrom(filename));
        } catch (Throwable ex) {
            throw wrapException(ex);
        }
        if (ret.equals(MemorySegment.NULL)) {
            return null;
        }
        return ret.getString(0);
    }

    private static final MethodHandle open = function(
            C_POINTER, "openslide_open", C_POINTER);

    static OpenSlideRef openslide_open(String filename) {
        if (filename == null) {
            return null;
        }
        MemorySegment ret;
        try (Arena arena = Arena.ofConfined()) {
            ret = (MemorySegment) open.invokeExact(
                    arena.allocateFrom(filename));
        } catch (Throwable ex) {
            throw wrapException(ex);
        }
        if (ret.equals(MemorySegment.NULL)) {
            return null;
        }
        return new OpenSlideRef(ret);
    }

    private static final MethodHandle get_level_count = function(
            JAVA_INT, "openslide_get_level_count", C_POINTER);

    static int openslide_get_level_count(OpenSlideRef osr) {
        try (Ref.ScopedLock l = osr.lock()) {
            return (int) get_level_count.invokeExact(osr.getSegment());
        } catch (Throwable ex) {
            throw wrapException(ex);
        }
    }

    private static final MethodHandle get_level_dimensions = function(
            null, "openslide_get_level_dimensions", C_POINTER, JAVA_INT,
            C_POINTER, C_POINTER);

    static void openslide_get_level_dimensions(OpenSlideRef osr, int level,
            long dim[]) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment w = arena.allocateFrom(JAVA_LONG, 0);
            MemorySegment h = arena.allocateFrom(JAVA_LONG, 0);
            try (Ref.ScopedLock l = osr.lock()) {
                get_level_dimensions.invokeExact(osr.getSegment(), level, w, h);
            } catch (Throwable ex) {
                throw wrapException(ex);
            }
            dim[0] = w.get(JAVA_LONG, 0);
            dim[1] = h.get(JAVA_LONG, 0);
        }
    }

    private static final MethodHandle get_level_downsample = function(
            JAVA_DOUBLE, "openslide_get_level_downsample", C_POINTER, JAVA_INT);

    static double openslide_get_level_downsample(OpenSlideRef osr, int level) {
        try (Ref.ScopedLock l = osr.lock()) {
            return (double) get_level_downsample.invokeExact(osr.getSegment(),
                    level);
        } catch (Throwable ex) {
            throw wrapException(ex);
        }
    }

    private static final MethodHandle read_region = function(
            null, "openslide_read_region", C_POINTER, C_POINTER,
            JAVA_LONG, JAVA_LONG, JAVA_INT, JAVA_LONG, JAVA_LONG);

    static void openslide_read_region(OpenSlideRef osr, int dest[],
            long x, long y, int level, long w, long h) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment buf = arena.allocate(JAVA_INT, dest.length);
            try (Ref.ScopedLock l = osr.lock()) {
                read_region.invokeExact(osr.getSegment(), buf, x, y,
                        level, w, h);
            } catch (Throwable ex) {
                throw wrapException(ex);
            }
            MemorySegment.copy(buf, JAVA_INT, 0, dest, 0, dest.length);
        }
    }

    private static final MethodHandle get_icc_profile_size = function(
            JAVA_LONG, "openslide_get_icc_profile_size", C_POINTER);

    static long openslide_get_icc_profile_size(OpenSlideRef osr) {
        try (Ref.ScopedLock l = osr.lock()) {
            return (long) get_icc_profile_size.invokeExact(osr.getSegment());
        } catch (Throwable ex) {
            throw wrapException(ex);
        }
    }

    private static final MethodHandle read_icc_profile = function(
            null, "openslide_read_icc_profile", C_POINTER, C_POINTER);

    static void openslide_read_icc_profile(OpenSlideRef osr, byte dest[]) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment buf = arena.allocate(JAVA_BYTE, dest.length);
            try (Ref.ScopedLock l = osr.lock()) {
                read_icc_profile.invokeExact(osr.getSegment(), buf);
            } catch (Throwable ex) {
                throw wrapException(ex);
            }
            MemorySegment.copy(buf, JAVA_BYTE, 0, dest, 0, dest.length);
        }
    }

    private static final MethodHandle get_error = function(
            C_POINTER, "openslide_get_error", C_POINTER);

    static String openslide_get_error(OpenSlideRef osr) {
        MemorySegment ret;
        try (Ref.ScopedLock l = osr.lock()) {
            ret = (MemorySegment) get_error.invokeExact(osr.getSegment());
        } catch (Throwable ex) {
            throw wrapException(ex);
        }
        if (ret.equals(MemorySegment.NULL)) {
            return null;
        }
        return ret.getString(0);
    }

    private static final MethodHandle get_property_names = function(
            C_POINTER, "openslide_get_property_names", C_POINTER);

    static String[] openslide_get_property_names(OpenSlideRef osr) {
        MemorySegment ret;
        try (Ref.ScopedLock l = osr.lock()) {
            ret = (MemorySegment) get_property_names.invokeExact(
                    osr.getSegment());
        } catch (Throwable ex) {
            throw wrapException(ex);
        }
        return segment_to_string_array(ret);
    }

    private static final MethodHandle get_property_value = function(
            C_POINTER, "openslide_get_property_value", C_POINTER, C_POINTER);

    static String openslide_get_property_value(OpenSlideRef osr, String name) {
        if (name == null) {
            return null;
        }
        MemorySegment ret;
        try (Arena arena = Arena.ofConfined(); Ref.ScopedLock l = osr.lock()) {
            ret = (MemorySegment) get_property_value.invokeExact(
                    osr.getSegment(), arena.allocateFrom(name));
        } catch (Throwable ex) {
            throw wrapException(ex);
        }
        if (ret.equals(MemorySegment.NULL)) {
            return null;
        }
        return ret.getString(0);
    }

    private static final MethodHandle get_associated_image_names = function(
            C_POINTER, "openslide_get_associated_image_names", C_POINTER);

    static String[] openslide_get_associated_image_names(OpenSlideRef osr) {
        MemorySegment ret;
        try (Ref.ScopedLock l = osr.lock()) {
            ret = (MemorySegment) get_associated_image_names.invokeExact(
                    osr.getSegment());
        } catch (Throwable ex) {
            throw wrapException(ex);
        }
        return segment_to_string_array(ret);
    }

    private static final MethodHandle get_associated_image_dimensions = function(
            null, "openslide_get_associated_image_dimensions", C_POINTER,
            C_POINTER, C_POINTER, C_POINTER);

    static void openslide_get_associated_image_dimensions(OpenSlideRef osr,
            String name, long dim[]) {
        if (name == null) {
            return;
        }
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment w = arena.allocateFrom(JAVA_LONG, 0);
            MemorySegment h = arena.allocateFrom(JAVA_LONG, 0);
            try (Ref.ScopedLock l = osr.lock()) {
                get_associated_image_dimensions.invokeExact(osr.getSegment(),
                        arena.allocateFrom(name), w, h);
            } catch (Throwable ex) {
                throw wrapException(ex);
            }
            dim[0] = w.get(JAVA_LONG, 0);
            dim[1] = h.get(JAVA_LONG, 0);
        }
    }

    private static final MethodHandle read_associated_image = function(
            null, "openslide_read_associated_image", C_POINTER, C_POINTER,
            C_POINTER);

    static void openslide_read_associated_image(OpenSlideRef osr, String name,
            int dest[]) {
        if (name == null) {
            return;
        }
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment buf = arena.allocate(JAVA_INT, dest.length);
            try (Ref.ScopedLock l = osr.lock()) {
                read_associated_image.invokeExact(osr.getSegment(),
                        arena.allocateFrom(name), buf);
            } catch (Throwable ex) {
                throw wrapException(ex);
            }
            MemorySegment.copy(buf, JAVA_INT, 0, dest, 0, dest.length);
        }
    }

    private static final MethodHandle get_associated_image_icc_profile_size = function(
            JAVA_LONG, "openslide_get_associated_image_icc_profile_size",
            C_POINTER, C_POINTER);

    static long openslide_get_associated_image_icc_profile_size(
            OpenSlideRef osr, String name) {
        if (name == null) {
            return -1;
        }
        try (Arena arena = Arena.ofConfined(); Ref.ScopedLock l = osr.lock()) {
            return (long) get_associated_image_icc_profile_size.invokeExact(
                    osr.getSegment(), arena.allocateFrom(name));
        } catch (Throwable ex) {
            throw wrapException(ex);
        }
    }

    private static final MethodHandle read_associated_image_icc_profile = function(
            null, "openslide_read_associated_image_icc_profile", C_POINTER,
            C_POINTER, C_POINTER);

    static void openslide_read_associated_image_icc_profile(OpenSlideRef osr,
            String name, byte dest[]) {
        if (name == null) {
            return;
        }
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment buf = arena.allocate(JAVA_BYTE, dest.length);
            try (Ref.ScopedLock l = osr.lock()) {
                read_associated_image_icc_profile.invokeExact(osr.getSegment(),
                        arena.allocateFrom(name), buf);
            } catch (Throwable ex) {
                throw wrapException(ex);
            }
            MemorySegment.copy(buf, JAVA_BYTE, 0, dest, 0, dest.length);
        }
    }

    private static final MethodHandle cache_create = function(
            C_POINTER, "openslide_cache_create", SIZE_T);

    static OpenSlideCacheRef openslide_cache_create(long capacity) {
        MemorySegment ret;
        try {
            ret = (MemorySegment) cache_create.invokeExact(capacity);
        } catch (Throwable ex) {
            throw wrapException(ex);
        }
        return new OpenSlideCacheRef(ret);
    }

    private static final MethodHandle set_cache = function(
            null, "openslide_set_cache", C_POINTER, C_POINTER);

    static void openslide_set_cache(OpenSlideRef osr, OpenSlideCacheRef cache) {
        try (Ref.ScopedLock cl = cache.lock(); Ref.ScopedLock ol = osr.lock()) {
            set_cache.invokeExact(osr.getSegment(), cache.getSegment());
        } catch (Throwable ex) {
            throw wrapException(ex);
        }
    }

    private static final MethodHandle get_version = function(
            C_POINTER, "openslide_get_version");

    static String openslide_get_version() {
        MemorySegment ret;
        try {
            ret = (MemorySegment) get_version.invokeExact();
        } catch (Throwable ex) {
            throw wrapException(ex);
        }
        return ret.getString(0);
    }
}
