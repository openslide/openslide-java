/*
 *  OpenSlide, a library for reading whole slide image files
 *
 *  Copyright (c) 2007-2010 Carnegie Mellon University
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

import java.io.InputStream;
import java.io.IOException;
import java.util.Properties;

class OpenSlideJNI {
    private OpenSlideJNI() {
    }

    static {
        String libraryPath = null;

        try {
            InputStream is = OpenSlideJNI.class.getClassLoader().
                    getResourceAsStream("resources/openslide.properties");
            if (is != null) {
                Properties p = new Properties();
                p.load(is);
                libraryPath = p.getProperty("openslide.jni.path");
                if (libraryPath.equals("")) {
                    libraryPath = null;
                }
            }
        } catch (SecurityException e1) {
            e1.printStackTrace();
        } catch (IOException e2) {
            e2.printStackTrace();
        }

        if (libraryPath != null) {
            System.load(libraryPath);
        } else {
            System.loadLibrary("openslide-jni");
        }
    }

    native static String openslide_detect_vendor(String file);

    native static long openslide_open(String file);

    native static int openslide_get_level_count(long osr);

    native static void openslide_get_level_dimensions(long osr, int level,
            long dim[]);

    native static double openslide_get_level_downsample(long osr, int level);

    native static void openslide_close(long osr);

    native static String[] openslide_get_property_names(long osr);

    native static String openslide_get_property_value(long osr, String name);

    native static String[] openslide_get_associated_image_names(long osr);

    native static void openslide_read_region(long osr, int dest[], long x,
            long y, int level, long w, long h);

    native static void openslide_get_associated_image_dimensions(long osr,
            String name, long dim[]);

    native static void openslide_read_associated_image(long osr, String name,
            int dest[]);

    native static String openslide_get_error(long osr);

    native static String openslide_get_version();
}
