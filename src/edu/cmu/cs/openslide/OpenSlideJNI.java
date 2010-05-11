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

package edu.cmu.cs.openslide;

class OpenSlideJNI {
    private OpenSlideJNI() {
    }

    native static boolean openslide_can_open(String file);

    native static long openslide_open(String file);

    native static int openslide_get_layer_count(long osr);

    native static void openslide_get_layer_dimensions(long osr, int layer,
            long dim[]);

    native static double openslide_get_layer_downsample(long osr, int layer);

    native static void openslide_close(long osr);

    native static String[] openslide_get_property_names(long osr);

    native static String openslide_get_property_value(long osr, String name);

    native static String[] openslide_get_associated_image_names(long osr);

    native static void openslide_read_region(long osr, int dest[], long x,
            long y, int layer, long w, long h);

    native static void openslide_get_associated_image_dimensions(long osr,
            String name, long dim[]);

    native static void openslide_read_associated_image(long osr, String name,
            int dest[]);

    native static String openslide_get_error(long osr);
}
