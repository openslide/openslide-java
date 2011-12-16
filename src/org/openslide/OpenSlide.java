/*
 *  OpenSlide, a library for reading whole slide image files
 *
 *  Copyright (c) 2007-2011 Carnegie Mellon University
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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.swing.filechooser.FileFilter;

public final class OpenSlide implements Closeable {
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

    final public static String PROPERTY_NAME_COMMENT = "openslide.comment";

    final public static String PROPERTY_NAME_VENDOR = "openslide.vendor";

    final public static String PROPERTY_NAME_QUICKHASH1 = "openslide.quickhash-1";

    final public static String PROPERTY_NAME_BACKGROUND_COLOR = "openslide.background-color";

    private long osr;

    final private ReadWriteLock lock = new ReentrantReadWriteLock();

    final private long layerWidths[];

    final private long layerHeights[];

    final private double layerDownsamples[];

    final private int layerCount;

    final private Map<String, String> properties;

    final private Map<String, AssociatedImage> associatedImages;

    final private int hashCodeVal;

    public static boolean fileIsValid(File file) {
        return OpenSlideJNI.openslide_can_open(file.getPath());
    }

    public OpenSlide(File file) throws IOException {
        if (!file.exists()) {
            throw new FileNotFoundException(file.toString());
        }

        osr = OpenSlideJNI.openslide_open(file.getPath());

        if (osr == 0) {
            throw new IOException(file
                    + ": Not a file that OpenSlide can recognize");
        }

        // store layer count
        layerCount = OpenSlideJNI.openslide_get_layer_count(osr);

        // store dimensions
        layerWidths = new long[layerCount];
        layerHeights = new long[layerCount];
        layerDownsamples = new double[layerCount];

        for (int i = 0; i < layerCount; i++) {
            long dim[] = new long[2];
            OpenSlideJNI.openslide_get_layer_dimensions(osr, i, dim);
            layerWidths[i] = dim[0];
            layerHeights[i] = dim[1];
            layerDownsamples[i] = OpenSlideJNI.openslide_get_layer_downsample(
                    osr, i);
        }

        // properties
        HashMap<String, String> props = new HashMap<String, String>();
        for (String s : OpenSlideJNI.openslide_get_property_names(osr)) {
            props.put(s, OpenSlideJNI.openslide_get_property_value(osr, s));
        }

        properties = Collections.unmodifiableMap(props);

        // associated images
        HashMap<String, AssociatedImage> associated =
                new HashMap<String, AssociatedImage>();
        for (String s : OpenSlideJNI
                .openslide_get_associated_image_names(osr)) {
            associated.put(s, new AssociatedImage(s, this));
        }

        associatedImages = Collections.unmodifiableMap(associated);

        // store hash
        hashCodeVal = (int) Long.parseLong(getProperties().get(
                PROPERTY_NAME_QUICKHASH1).substring(0, 8), 16);

        // dispose on error, we are in the constructor
        try {
            checkError();
        } catch (IOException e) {
            dispose();
            throw e;
        }
    }

    // call with the reader lock held, or from the constructor
    private void checkError() throws IOException {
        String msg = OpenSlideJNI.openslide_get_error(osr);

        if (msg != null) {
            throw new IOException(msg);
        }
    }

    // takes the writer lock
    public void dispose() {
        Lock wl = lock.writeLock();
        wl.lock();
        try {
            if (osr != 0) {
                OpenSlideJNI.openslide_close(osr);
                osr = 0;
            }
        } finally {
            wl.unlock();
        }
    }

    public int getLayerCount() {
        return layerCount;
    }

    // call with the reader lock held
    private void checkDisposed() {
        if (osr == 0) {
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
        return properties.get(PROPERTY_NAME_COMMENT);
    }

    public void paintRegionOfLayer(Graphics2D g, int dx, int dy, int sx,
            int sy, int w, int h, int layer) throws IOException {
        paintRegion(g, dx, dy, sx, sy, w, h, layerDownsamples[layer]);
    }

    // takes the reader lock
    public void paintRegionARGB(int dest[], long x, long y, int layer, int w,
            int h) throws IOException {
        if ((long) w * (long) h > dest.length) {
            throw new ArrayIndexOutOfBoundsException("Size of data ("
                    + dest.length + ") is less than w * h");
        }

        if (w < 0 || h < 0) {
            throw new IllegalArgumentException("w and h must be nonnegative");
        }

        Lock rl = lock.readLock();
        rl.lock();
        try {
            checkDisposed();
            OpenSlideJNI.openslide_read_region(osr, dest, x, y, layer, w, h);
            checkError();
        } finally {
            rl.unlock();
        }
    }

    public void paintRegion(Graphics2D g, int dx, int dy, long sx, long sy,
            int w, int h, double downsample) throws IOException {
        if (downsample < 1.0) {
            throw new IllegalArgumentException("downsample (" + downsample
                    + ") must be >= 1.0");
        }

        // get the layer
        int layer = getBestLayerForDownsample(downsample);

        // figure out its downsample
        double layerDS = layerDownsamples[layer];

        // compute the difference
        double relativeDS = downsample / layerDS;

        // scale source coordinates into layer coordinates
        long baseX = (long) (downsample * sx);
        long baseY = (long) (downsample * sy);
        long layerX = (long) (relativeDS * sx);
        long layerY = (long) (relativeDS * sy);

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

        paintRegionARGB(data, baseX, baseY, layer, img.getWidth(), img
                .getHeight());

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
            int maxSize, int bufferedImageType) throws IOException {
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
            int maxSize) throws IOException {
        return createThumbnailImage(x, y, w, h, maxSize,
                BufferedImage.TYPE_INT_RGB);
    }

    public BufferedImage createThumbnailImage(int maxSize) throws IOException {
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

    public Map<String, AssociatedImage> getAssociatedImages() {
        return associatedImages;
    }

    // takes the reader lock
    BufferedImage getAssociatedImage(String name) throws IOException {
        Lock rl = lock.readLock();
        rl.lock();
        try {
            checkDisposed();

            long dim[] = new long[2];
            OpenSlideJNI.openslide_get_associated_image_dimensions(osr, name,
                    dim);
            checkError();
            if (dim[0] == -1) {
                // non-terminal error
                throw new IOException("Failure reading associated image");
            }

            BufferedImage img = new BufferedImage((int) dim[0], (int) dim[1],
                    BufferedImage.TYPE_INT_ARGB_PRE);

            int data[] = ((DataBufferInt) img.getRaster().getDataBuffer())
                    .getData();

            OpenSlideJNI.openslide_read_associated_image(osr, name, data);
            checkError();
            return img;
        } finally {
            rl.unlock();
        }
    }

    public static FileFilter getFileFilter() {
        return FILE_FILTER;
    }

    @Override
    public int hashCode() {
        return hashCodeVal;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj instanceof OpenSlide) {
            OpenSlide os2 = (OpenSlide) obj;
            return getProperties().get(PROPERTY_NAME_QUICKHASH1).equals(
                    os2.getProperties().get(PROPERTY_NAME_QUICKHASH1));
        }

        return false;
    }

    @Override
    public void close() {
        dispose();
    }
}
