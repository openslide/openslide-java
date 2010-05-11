/*
 *  OpenSlide, a library for reading whole slide image files
 *
 *  Copyright (c) 2007-2009 Carnegie Mellon University
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

import java.io.File;
import java.io.IOException;

public class TestCLI {
    static void print_downsamples(OpenSlide osr) {
        for (int layer = 0; layer < osr.getLayerCount(); layer++) {
            System.out.printf("layer %d: downsample: %g\n", layer, osr
                    .getLayerDownsample(layer));
        }
    }

    static void test_next_biggest(OpenSlide osr, double downsample) {
        int layer = osr.getBestLayerForDownsample(downsample);
        System.out.printf("layer for downsample %g: %d (%g)\n", downsample,
                layer, osr.getLayerDownsample(layer));
    }

    public static void main(String args[]) throws IOException {
        if (args.length != 1) {
            System.out.printf("give file!\n");
            return;
        }

        File f = new File(args[0]);

        System.out.printf("openslide_can_open returns %s\n", OpenSlide
                .fileIsValid(f) ? "true" : "false");
        OpenSlide osr = new OpenSlide(f);

        long w, h;

        osr.dispose();

        osr = new OpenSlide(f);

        w = osr.getLayer0Width();
        h = osr.getLayer0Height();
        System.out.printf("dimensions: %d x %d\n", w, h);
        System.out.printf("comment: %s\n", osr.getComment());

        int layers = osr.getLayerCount();
        System.out.printf("num layers: %d\n", layers);

        print_downsamples(osr);

        test_next_biggest(osr, 0.8);
        test_next_biggest(osr, 1.0);
        test_next_biggest(osr, 1.5);
        test_next_biggest(osr, 2.0);
        test_next_biggest(osr, 3.0);
        test_next_biggest(osr, 3.1);
        test_next_biggest(osr, 10);
        test_next_biggest(osr, 20);
        test_next_biggest(osr, 25);
        test_next_biggest(osr, 100);
        test_next_biggest(osr, 1000);
        test_next_biggest(osr, 10000);

        osr.dispose();
    }
}