/*
 *  OpenSlide, a library for reading whole slide image files
 *
 *  Copyright (c) 2007-2009 Carnegie Mellon University
 *  All rights reserved.
 *
 *  OpenSlide is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, version 2.
 *
 *  OpenSlide is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with OpenSlide. If not, see <http://www.gnu.org/licenses/>.
 *
 *  Linking OpenSlide statically or dynamically with other modules is
 *  making a combined work based on OpenSlide. Thus, the terms and
 *  conditions of the GNU General Public License cover the whole
 *  combination.
 */

package edu.cmu.cs.openslide;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.util.*;

import javax.swing.filechooser.FileFilter;

public class OpenSlide {
    private static final FileFilter FILE_FILTER = new FileFilter() {
        @Override
        public boolean accept(File f) {
            return f.isDirectory() || OpenSlide.fileIsValid(f);
        }

        @Override
        public String getDescription() {
            return "Virtual slide";
        }
    };

    private SWIGTYPE_p__openslide osr;

    final private long layerWidths[];

    final private long layerHeights[];

    final private double layerDownsamples[];

    final private int layerCount;

    final private Map<String, String> properties;

    final private AssociatedImageMap associatedImages;

    public static boolean fileIsValid(File file) {
        return OpenSlideGlue.openslide_can_open(file.getPath());
    }

    public OpenSlide(File file) {
        osr = OpenSlideGlue.openslide_open(file.getPath());

        if (osr == null) {
            // TODO not just file not found
            throw new OpenSlideException();
        }

        // store layer count
        layerCount = OpenSlideGlue.openslide_get_layer_count(osr);

        // store dimensions
        layerWidths = new long[layerCount];
        layerHeights = new long[layerCount];
        layerDownsamples = new double[layerCount];

        for (int i = 0; i < layerCount; i++) {
            long w[] = new long[1];
            long h[] = new long[1];
            OpenSlideGlue.openslide_get_layer_dimensions(osr, i, w, h);
            layerWidths[i] = w[0];
            layerHeights[i] = h[0];
            layerDownsamples[i] = OpenSlideGlue.openslide_get_layer_downsample(
                    osr, i);
        }

        // properties
        HashMap<String, String> props = new HashMap<String, String>();
        SWIGTYPE_p_p_char propNames = OpenSlideGlue
                .openslide_get_property_names(osr);
        int i = 0;
        String currentName;
        while ((currentName = OpenSlideGlue.deref_char_p_p(propNames, i++)) != null) {
            String value = OpenSlideGlue.openslide_get_property_value(osr,
                    currentName);
            props.put(currentName, value);
        }
        properties = Collections.unmodifiableMap(props);

        // associated images
        // make names
        Set<String> names = new HashSet<String>();
        SWIGTYPE_p_p_char imgNames = OpenSlideGlue
                .openslide_get_associated_image_names(osr);
        i = 0;
        while ((currentName = OpenSlideGlue.deref_char_p_p(imgNames, i++)) != null) {
            names.add(currentName);
        }

        associatedImages = new AssociatedImageMap(Collections
                .unmodifiableSet(names), this);
    }

    public void dispose() {
        if (osr != null) {
            OpenSlideGlue.openslide_close(osr);
            osr = null;
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        dispose();
    }

    public int getLayerCount() {
        return layerCount;
    }

    private void checkDisposed() {
        if (osr == null) {
            throw new OpenSlideDisposedException();
        }
    }

    public long getLayer0Width() {
        return layerWidths[0];
    }

    public long getLayer0Height() {
        return layerHeights[0];
    }

    public long getLayerWidth(int layer) {
        return layerWidths[layer];
    }

    public long getLayerHeight(int layer) {
        return layerHeights[layer];
    }

    public String getComment() {
        checkDisposed();

        return OpenSlideGlue.openslide_get_comment(osr);
    }

    public void paintRegion(Graphics2D g, int dx, int dy, int sx, int sy,
            int w, int h, double downsample) {
        checkDisposed();

        if (downsample < 1.0) {
            throw new IllegalArgumentException("downsample (" + downsample
                    + ") must be >= 1.0");
        }

        // get the layer
        int layer = OpenSlideGlue.openslide_get_best_layer_for_downsample(osr,
                downsample);

        // figure out its downsample
        double layerDS = OpenSlideGlue.openslide_get_layer_downsample(osr,
                layer);

        // compute the difference
        double relativeDS = downsample / layerDS;

        // translate if sx or sy are negative
        if (sx < 0) {
            dx -= sx;
            w += sx; // shrink w
            sx = 0;
        }
        if (sy < 0) {
            dy -= sy;
            h += sy; // shrink h
            sy = 0;
        }

        // scale source coordinates into layer coordinates
        int baseX = (int) (downsample * sx);
        int baseY = (int) (downsample * sy);
        int layerX = (int) (relativeDS * sx);
        int layerY = (int) (relativeDS * sy);

        // scale width and height by relative downsample
        int layerW = (int) Math.round(relativeDS * w);
        int layerH = (int) Math.round(relativeDS * h);

        // clip to edge of image
        layerW = (int) Math.min(layerW, getLayerWidth(layer) - layerX);
        layerH = (int) Math.min(layerH, getLayerHeight(layer) - layerY);
        w = (int) Math.round(layerW / relativeDS);
        h = (int) Math.round(layerH / relativeDS);

        if (debug) {
            System.out.println("layerW " + layerW + ", layerH " + layerH
                    + ", baseX " + baseX + ", baseY " + baseY);
        }

        if (layerW <= 0 || layerH <= 0) {
            // nothing to draw
            return;
        }

        BufferedImage img = new BufferedImage(layerW, layerH,
                BufferedImage.TYPE_INT_ARGB_PRE);

        int data[] = ((DataBufferInt) img.getRaster().getDataBuffer())
                .getData();

        OpenSlideGlue.openslide_read_region(osr, data, baseX, baseY, layer, img
                .getWidth(), img.getHeight());

        // g.scale(1.0 / relativeDS, 1.0 / relativeDS);
        g.drawImage(img, dx, dy, w, h, null);

        if (debug) {
            System.out.println(img);

            if (debugThingy == 0) {
                g.setColor(new Color(1.0f, 0.0f, 0.0f, 0.4f));
                debugThingy = 1;
            } else {
                g.setColor(new Color(0.0f, 1.0f, 0.0f, 0.4f));
                debugThingy = 0;
            }
            g.fillRect(dx, dy, w, h);
        }
    }

    final boolean debug = false;

    private int debugThingy = 0;

    public BufferedImage createThumbnailImage(int x, int y, long w, long h,
            int maxSize, int bufferedImageType) {
        double ds;

        if (w > h) {
            ds = (double) w / maxSize;
        } else {
            ds = (double) h / maxSize;
        }

        if (ds < 1.0) {
            ds = 1.0;
        }

        int sw = (int) (w / ds);
        int sh = (int) (h / ds);
        int sx = (int) (x / ds);
        int sy = (int) (y / ds);

        BufferedImage result = new BufferedImage(sw, sh, bufferedImageType);

        Graphics2D g = result.createGraphics();
        paintRegion(g, 0, 0, sx, sy, sw, sh, ds);
        g.dispose();
        return result;
    }

    public BufferedImage createThumbnailImage(int x, int y, long w, long h,
            int maxSize) {
        return createThumbnailImage(x, y, w, h, maxSize,
                BufferedImage.TYPE_INT_RGB);
    }

    public BufferedImage createThumbnailImage(int maxSize) {
        return createThumbnailImage(0, 0, getLayer0Width(), getLayer0Height(),
                maxSize);
    }

    public double getLayerDownsample(int layer) {
        return layerDownsamples[layer];
    }

    public int getBestLayerForDownsample(double downsample) {
        // too small, return first
        if (downsample < layerDownsamples[0]) {
            return 0;
        }

        // find where we are in the middle
        for (int i = 1; i < layerCount; i++) {
            if (downsample < layerDownsamples[i]) {
                return i - 1;
            }
        }

        // too big, return last
        return layerCount - 1;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public Map<String, BufferedImage> getAssociatedImages() {
        return associatedImages;
    }

    BufferedImage getAssociatedImage(String name) {
        long ww[] = new long[1];
        long hh[] = new long[1];
        OpenSlideGlue.openslide_get_associated_image_dimensions(osr, name, ww,
                hh);

        BufferedImage img = new BufferedImage((int) ww[0], (int) hh[0],
                BufferedImage.TYPE_INT_ARGB_PRE);

        int data[] = ((DataBufferInt) img.getRaster().getDataBuffer())
                .getData();

        OpenSlideGlue.openslide_read_associated_image(osr, name, data);

        return img;
    }

    public static FileFilter getFileFilter() {
        return FILE_FILTER;
    }
}
