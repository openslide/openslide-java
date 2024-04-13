/*
 *  OpenSlide, a library for reading whole slide image files
 *
 *  Copyright (c) 2007-2011 Carnegie Mellon University
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

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public final class OpenSlideCache implements AutoCloseable {
    private final Lock lock = new ReentrantLock();

    private OpenSlideFFM.OpenSlideCacheRef cache;

    public OpenSlideCache(long capacity) {
        cache = OpenSlideFFM.openslide_cache_create(capacity);
    }

    Lock getLock() {
        return lock;
    }

    // call, and use result, with lock held
    OpenSlideFFM.OpenSlideCacheRef getRef() {
        if (cache == null) {
            throw new OpenSlideDisposedException("OpenSlideCache");
        }
        return cache;
    }

    // takes the lock
    @Override
    public void close() {
        lock.lock();
        try {
            if (cache != null) {
                OpenSlideFFM.openslide_cache_release(cache);
                cache = null;
            }
        } finally {
            lock.unlock();
        }
    }
}
