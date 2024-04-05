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

@SuppressWarnings("restricted")
class OpenSlideFFM {
    private static final Arena LIBRARY_ARENA = Arena.ofAuto();

    private static final SymbolLookup SYMBOL_LOOKUP = getSymbolLookup();

    private static final AddressLayout C_POINTER = ADDRESS.withTargetLayout(
            MemoryLayout.sequenceLayout(Long.MAX_VALUE, JAVA_BYTE));

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
        for (int ver = 1; ver >= 0; ver--) {
            String lname;
            if (tmpl.endsWith(".dll")) {
                lname = String.format("libopenslide-%d.dll", ver);
            } else if (tmpl.endsWith(".dylib")) {
                lname = String.format("libopenslide.%d.dylib", ver);
            } else {
                lname = String.format("%s.%d", tmpl, ver);
            }
            try {
                return SymbolLookup.libraryLookup(lname, LIBRARY_ARENA);
            } catch (IllegalArgumentException ex) {
                continue;
            }
        }
        throw new UnsatisfiedLinkError(
                "Couldn't locate OpenSlide library; add it to the operating " +
                "system's library search path or use System.load() or " +
                "System.loadLibrary()");
    }

    private static MethodHandle function(MemoryLayout ret, String name,
            MemoryLayout... args) {
        MemorySegment symbol = SYMBOL_LOOKUP.find(name)
                .orElseThrow(() -> new UnsatisfiedLinkError(
                "Unresolved symbol " + name + "; need OpenSlide >= 3.4.0"));
        FunctionDescriptor desc;
        if (ret != null) {
            desc = FunctionDescriptor.of(ret, args);
        } else {
            desc = FunctionDescriptor.ofVoid(args);
        }
        return Linker.nativeLinker().downcallHandle(symbol, desc);
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
           throw new AssertionError("Invalid call", ex);
        }
        if (ret.equals(MemorySegment.NULL)) {
            return null;
        }
        return ret.getString(0);
    }

    private static final MethodHandle open = function(
            C_POINTER, "openslide_open", C_POINTER);

    static MemorySegment openslide_open(String filename) {
        if (filename == null) {
            return null;
        }
        MemorySegment ret;
        try (Arena arena = Arena.ofConfined()) {
            ret = (MemorySegment) open.invokeExact(
                    arena.allocateFrom(filename));
        } catch (Throwable ex) {
           throw new AssertionError("Invalid call", ex);
        }
        if (ret.equals(MemorySegment.NULL)) {
            return null;
        }
        return ret;
    }

    private static final MethodHandle get_level_count = function(
            JAVA_INT, "openslide_get_level_count", C_POINTER);

    static int openslide_get_level_count(MemorySegment osr) {
        try {
            return (int) get_level_count.invokeExact(osr);
        } catch (Throwable ex) {
           throw new AssertionError("Invalid call", ex);
        }
    }

    private static final MethodHandle get_level_dimensions = function(
            null, "openslide_get_level_dimensions", C_POINTER, JAVA_INT,
            C_POINTER, C_POINTER);

    static void openslide_get_level_dimensions(MemorySegment osr, int level,
            long dim[]) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment w = arena.allocateFrom(JAVA_LONG, 0);
            MemorySegment h = arena.allocateFrom(JAVA_LONG, 0);
            try {
                get_level_dimensions.invokeExact(osr, level, w, h);
            } catch (Throwable ex) {
               throw new AssertionError("Invalid call", ex);
            }
            dim[0] = w.get(JAVA_LONG, 0);
            dim[1] = h.get(JAVA_LONG, 0);
        }
    }

    private static final MethodHandle get_level_downsample = function(
            JAVA_DOUBLE, "openslide_get_level_downsample", C_POINTER, JAVA_INT);

    static double openslide_get_level_downsample(MemorySegment osr, int level) {
        try {
            return (double) get_level_downsample.invokeExact(osr, level);
        } catch (Throwable ex) {
           throw new AssertionError("Invalid call", ex);
        }
    }

    private static final MethodHandle read_region = function(
            null, "openslide_read_region", C_POINTER, C_POINTER,
            JAVA_LONG, JAVA_LONG, JAVA_INT, JAVA_LONG, JAVA_LONG);

    static void openslide_read_region(MemorySegment osr, int dest[],
            long x, long y, int level, long w, long h) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment buf = arena.allocate(JAVA_INT, dest.length);
            try {
                read_region.invokeExact(osr, buf, x, y, level, w, h);
            } catch (Throwable ex) {
               throw new AssertionError("Invalid call", ex);
            }
            MemorySegment.copy(buf, JAVA_INT, 0, dest, 0, dest.length);
        }
    }

    private static final MethodHandle close = function(
            null, "openslide_close", C_POINTER);

    static void openslide_close(MemorySegment osr) {
        try {
            close.invokeExact(osr);
        } catch (Throwable ex) {
           throw new AssertionError("Invalid call", ex);
        }
    }

    private static final MethodHandle get_error = function(
            C_POINTER, "openslide_get_error", C_POINTER);

    static String openslide_get_error(MemorySegment osr) {
        MemorySegment ret;
        try {
            ret = (MemorySegment) get_error.invokeExact(osr);
        } catch (Throwable ex) {
           throw new AssertionError("Invalid call", ex);
        }
        if (ret.equals(MemorySegment.NULL)) {
            return null;
        }
        return ret.getString(0);
    }

    private static final MethodHandle get_property_names = function(
            C_POINTER, "openslide_get_property_names", C_POINTER);

    static String[] openslide_get_property_names(MemorySegment osr) {
        MemorySegment ret;
        try {
            ret = (MemorySegment) get_property_names.invokeExact(osr);
        } catch (Throwable ex) {
           throw new AssertionError("Invalid call", ex);
        }
        return segment_to_string_array(ret);
    }

    private static final MethodHandle get_property_value = function(
            C_POINTER, "openslide_get_property_value", C_POINTER, C_POINTER);

    static String openslide_get_property_value(MemorySegment osr, String name) {
        if (name == null) {
            return null;
        }
        MemorySegment ret;
        try (Arena arena = Arena.ofConfined()) {
            ret = (MemorySegment) get_property_value.invokeExact(osr,
                    arena.allocateFrom(name));
        } catch (Throwable ex) {
           throw new AssertionError("Invalid call", ex);
        }
        if (ret.equals(MemorySegment.NULL)) {
            return null;
        }
        return ret.getString(0);
    }

    private static final MethodHandle get_associated_image_names = function(
            C_POINTER, "openslide_get_associated_image_names", C_POINTER);

    static String[] openslide_get_associated_image_names(MemorySegment osr) {
        MemorySegment ret;
        try {
            ret = (MemorySegment) get_associated_image_names.invokeExact(osr);
        } catch (Throwable ex) {
           throw new AssertionError("Invalid call", ex);
        }
        return segment_to_string_array(ret);
    }

    private static final MethodHandle get_associated_image_dimensions = function(
            null, "openslide_get_associated_image_dimensions", C_POINTER,
            C_POINTER, C_POINTER, C_POINTER);

    static void openslide_get_associated_image_dimensions(MemorySegment osr,
            String name, long dim[]) {
        if (name == null) {
            return;
        }
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment w = arena.allocateFrom(JAVA_LONG, 0);
            MemorySegment h = arena.allocateFrom(JAVA_LONG, 0);
            try {
                get_associated_image_dimensions.invokeExact(osr,
                        arena.allocateFrom(name), w, h);
            } catch (Throwable ex) {
               throw new AssertionError("Invalid call", ex);
            }
            dim[0] = w.get(JAVA_LONG, 0);
            dim[1] = h.get(JAVA_LONG, 0);
        }
    }

    private static final MethodHandle read_associated_image = function(
            null, "openslide_read_associated_image", C_POINTER, C_POINTER,
            C_POINTER);

    static void openslide_read_associated_image(MemorySegment osr, String name,
            int dest[]) {
        if (name == null) {
            return;
        }
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment buf = arena.allocate(JAVA_INT, dest.length);
            try {
                read_associated_image.invokeExact(osr, arena.allocateFrom(name),
                        buf);
            } catch (Throwable ex) {
               throw new AssertionError("Invalid call", ex);
            }
            MemorySegment.copy(buf, JAVA_INT, 0, dest, 0, dest.length);
        }
    }

    private static final MethodHandle get_version = function(
            C_POINTER, "openslide_get_version");

    static String openslide_get_version() {
        MemorySegment ret;
        try {
            ret = (MemorySegment) get_version.invokeExact();
        } catch (Throwable ex) {
           throw new AssertionError("Invalid call", ex);
        }
        return ret.getString(0);
    }
}
