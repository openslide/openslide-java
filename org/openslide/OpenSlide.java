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

import java.awt.Color;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.awt.image.DirectColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

import javax.swing.filechooser.FileFilter;

public final class OpenSlide implements Closeable {
    private static final FileFilter FILE_FILTER = new FileFilter() {
        @Override
        public boolean accept(File f) {
            return f.isDirectory() || OpenSlide.detectVendor(f) != null;
        }

        @Override
        public String getDescription() {
            return "Virtual slide";
        }
    };

    private static class ErrorCtx implements AutoCloseable {
        private final OpenSlideFFM.OpenSlideRef osr;

        ErrorCtx(OpenSlideFFM.OpenSlideRef osr) throws IOException {
            this.osr = osr;
            // immediately check for errors
            close();
        }

        OpenSlideFFM.OpenSlideRef getOsr() {
            return osr;
        }

        // doesn't really close; just a convenience wrapper for checking errors
        // via try-with-resources
        @Override
        public void close() throws IOException {
            String msg = OpenSlideFFM.openslide_get_error(osr);
            if (msg != null) {
                throw new IOException(msg);
            }
        }
    }

    private static final String LIBRARY_VERSION = OpenSlideFFM
            .openslide_get_version();

    final public static String PROPERTY_NAME_BACKGROUND_COLOR = "openslide.background-color";

    final public static String PROPERTY_NAME_BOUNDS_HEIGHT = "openslide.bounds-height";

    final public static String PROPERTY_NAME_BOUNDS_WIDTH = "openslide.bounds-width";

    final public static String PROPERTY_NAME_BOUNDS_X = "openslide.bounds-x";

    final public static String PROPERTY_NAME_BOUNDS_Y = "openslide.bounds-y";

    final public static String PROPERTY_NAME_COMMENT = "openslide.comment";

    final public static String PROPERTY_NAME_ICC_SIZE = "openslide.icc-size";

    final public static String PROPERTY_NAME_MPP_X = "openslide.mpp-x";

    final public static String PROPERTY_NAME_MPP_Y = "openslide.mpp-y";

    final public static String PROPERTY_NAME_OBJECTIVE_POWER = "openslide.objective-power";

    final public static String PROPERTY_NAME_QUICKHASH1 = "openslide.quickhash-1";

    final public static String PROPERTY_NAME_VENDOR = "openslide.vendor";

    // should generally be used in a try-with-resources block to detect errors
    final private ErrorCtx errorCtx;

    final private long levelWidths[];

    final private long levelHeights[];

    final private double levelDownsamples[];

    final private int levelCount;

    final private Map<String, String> properties;

    final private Map<String, AssociatedImage> associatedImages;

    final private ColorModel colorModel;

    final private File canonicalFile;

    final private int hashCodeVal;

    public static String detectVendor(File file) {
        return OpenSlideFFM.openslide_detect_vendor(file.getPath());
    }

    public OpenSlide(File file) throws IOException {
        // allow opening the synthetic slide
        if (!file.exists() && !file.getPath().equals("")) {
            throw new FileNotFoundException(file.toString());
        }

        OpenSlideFFM.OpenSlideRef osr = OpenSlideFFM.openslide_open(
                file.getPath());

        if (osr == null) {
            throw new IOException(file
                    + ": Not a file that OpenSlide can recognize");
        }
        try {
            errorCtx = new ErrorCtx(osr);
        } catch (IOException e) {
            // close, we are in the constructor
            osr.close();
            throw e;
        }

        try (errorCtx) {
            // store level count
            levelCount = OpenSlideFFM.openslide_get_level_count(osr);

            // store dimensions
            levelWidths = new long[levelCount];
            levelHeights = new long[levelCount];
            levelDownsamples = new double[levelCount];

            for (int i = 0; i < levelCount; i++) {
                long dim[] = new long[2];
                OpenSlideFFM.openslide_get_level_dimensions(osr, i, dim);
                levelWidths[i] = dim[0];
                levelHeights[i] = dim[1];
                levelDownsamples[i] =
                        OpenSlideFFM.openslide_get_level_downsample(osr, i);
            }

            // properties
            HashMap<String, String> props = new HashMap<String, String>();
            for (String s : OpenSlideFFM.openslide_get_property_names(osr)) {
                props.put(s, OpenSlideFFM.openslide_get_property_value(osr, s));
            }

            properties = Collections.unmodifiableMap(props);

            // associated images
            HashMap<String, AssociatedImage> associated =
                    new HashMap<String, AssociatedImage>();
            for (String s : OpenSlideFFM
                    .openslide_get_associated_image_names(osr)) {
                associated.put(s, new AssociatedImage(s, this));
            }

            associatedImages = Collections.unmodifiableMap(associated);

            colorModel = readColorModel(null);

            // store info for hash and equals
            canonicalFile = file.getCanonicalFile();
            String quickhash1 = getProperties().get(PROPERTY_NAME_QUICKHASH1);
            if (quickhash1 != null) {
                hashCodeVal = (int) Long.parseLong(quickhash1.substring(0, 8),
                        16);
            } else {
                hashCodeVal = canonicalFile.hashCode();
            }
        } catch (IOException e) {
            // close, we are in the constructor
            close();
            throw e;
        }
    }

    @Override
    public void close() {
        errorCtx.getOsr().close();
    }

    @Deprecated
    public void dispose() {
        close();
    }

    public int getLevelCount() {
        return levelCount;
    }

    public long getLevel0Width() {
        return levelWidths[0];
    }

    public long getLevel0Height() {
        return levelHeights[0];
    }

    public long getLevelWidth(int level) {
        return levelWidths[level];
    }

    public long getLevelHeight(int level) {
        return levelHeights[level];
    }

    public ColorModel getColorModel() {
        return colorModel;
    }

    public void paintRegionOfLevel(Graphics2D g, int dx, int dy, int sx,
            int sy, int w, int h, int level) throws IOException {
        paintRegion(g, dx, dy, sx, sy, w, h, levelDownsamples[level]);
    }

    public void paintRegionARGB(int dest[], long x, long y, int level, int w,
            int h) throws IOException {
        if ((long) w * (long) h > dest.length) {
            throw new ArrayIndexOutOfBoundsException("Size of data ("
                    + dest.length + ") is less than w * h");
        }

        if (w < 0 || h < 0) {
            throw new IllegalArgumentException("w and h must be nonnegative");
        }

        try (errorCtx) {
            OpenSlideFFM.openslide_read_region(errorCtx.getOsr(), dest, x, y,
                    level, w, h);
        }
    }

    public BufferedImage readRegion(long x, long y, int level, int w, int h)
            throws IOException {
        BufferedImage img = createARGBBufferedImage(colorModel, w, h);
        int data[] = getARGBPixels(img);
        paintRegionARGB(data, x, y, level, w, h);
        return img;
    }

    public void paintRegion(Graphics2D g, int dx, int dy, long sx, long sy,
            int w, int h, double downsample) throws IOException {
        if (downsample < 1.0) {
            throw new IllegalArgumentException("downsample (" + downsample
                    + ") must be >= 1.0");
        }

        // get the level
        int level = getBestLevelForDownsample(downsample);

        // figure out its downsample
        double levelDS = levelDownsamples[level];

        // compute the difference
        double relativeDS = downsample / levelDS;

        // scale source coordinates into level coordinates
        long baseX = (long) (downsample * sx);
        long baseY = (long) (downsample * sy);
        long levelX = (long) (relativeDS * sx);
        long levelY = (long) (relativeDS * sy);

        // scale width and height by relative downsample
        int levelW = (int) Math.round(relativeDS * w);
        int levelH = (int) Math.round(relativeDS * h);

        // clip to edge of image
        levelW = (int) Math.min(levelW, getLevelWidth(level) - levelX);
        levelH = (int) Math.min(levelH, getLevelHeight(level) - levelY);
        w = (int) Math.round(levelW / relativeDS);
        h = (int) Math.round(levelH / relativeDS);

        if (debug) {
            System.out.println("levelW " + levelW + ", levelH " + levelH
                    + ", baseX " + baseX + ", baseY " + baseY);
        }

        if (levelW <= 0 || levelH <= 0) {
            // nothing to draw
            return;
        }

        BufferedImage img = readRegion(baseX, baseY, level, levelW, levelH);

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
        return createThumbnailImage(0, 0, getLevel0Width(), getLevel0Height(),
                maxSize);
    }

    public double getLevelDownsample(int level) {
        return levelDownsamples[level];
    }

    public int getBestLevelForDownsample(double downsample) {
        // too small, return first
        if (downsample < levelDownsamples[0]) {
            return 0;
        }

        // find where we are in the middle
        for (int i = 1; i < levelCount; i++) {
            if (downsample < levelDownsamples[i]) {
                return i - 1;
            }
        }

        // too big, return last
        return levelCount - 1;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public Map<String, AssociatedImage> getAssociatedImages() {
        return associatedImages;
    }

    BufferedImage getAssociatedImage(String name) throws IOException {
        long dim[] = new long[2];
        try (errorCtx) {
            OpenSlideFFM.openslide_get_associated_image_dimensions(
                    errorCtx.getOsr(), name, dim);
        }
        if (dim[0] == -1) {
            // non-terminal error
            throw new IOException("Failure reading associated image");
        }

        ColorModel cm = readColorModel(name);
        BufferedImage img = createARGBBufferedImage(cm, (int) dim[0],
                (int) dim[1]);
        int data[] = getARGBPixels(img);

        try (errorCtx) {
            OpenSlideFFM.openslide_read_associated_image(errorCtx.getOsr(),
                    name, data);
        }
        return img;
    }

    public void setCache(OpenSlideCache cache) {
        // don't bother checking for OpenSlide errors
        OpenSlideFFM.openslide_set_cache(errorCtx.getOsr(), cache.getRef());
    }

    public static String getLibraryVersion() {
        return LIBRARY_VERSION;
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
            String quickhash1 = getProperties()
                    .get(PROPERTY_NAME_QUICKHASH1);
            String os2_quickhash1 = os2.getProperties()
                    .get(PROPERTY_NAME_QUICKHASH1);

            if (quickhash1 != null && os2_quickhash1 != null) {
                return quickhash1.equals(os2_quickhash1);
            } else if (quickhash1 == null && os2_quickhash1 == null) {
                return canonicalFile.equals(os2.canonicalFile);
            } else {
                return false;
            }
        }

        return false;
    }

    private static BufferedImage createARGBBufferedImage(ColorModel cm, int w,
            int h) {
        WritableRaster raster = Raster.createWritableRaster(
                cm.createCompatibleSampleModel(w, h), null);
        return new BufferedImage(cm, raster, true, null);
    }

    private static int[] getARGBPixels(BufferedImage img) {
        DataBufferInt buf = (DataBufferInt) img.getRaster().getDataBuffer();
        return buf.getData();
    }

    private ColorModel readColorModel(String associated) throws IOException {
        ColorSpace space = readColorSpace(associated);
        return new DirectColorModel(space, 32,
                0x00ff0000, 0x0000ff00, 0x000000ff, 0xff000000, true,
                DataBuffer.TYPE_INT);
    }

    private ColorSpace readColorSpace(String associated) throws IOException {
        long size;
        try (errorCtx) {
            if (associated != null) {
                size = OpenSlideFFM.openslide_get_associated_image_icc_profile_size(
                        errorCtx.getOsr(), associated);
            } else {
                size = OpenSlideFFM.openslide_get_icc_profile_size(
                        errorCtx.getOsr());
            }
        }
        if (size <= 0) {
            return ColorSpace.getInstance(ColorSpace.CS_sRGB);
        } else if (size > Integer.MAX_VALUE) {
            throw new IOException("ICC profile too large");
        }

        byte[] data = new byte[(int) size];
        try (errorCtx) {
            if (associated != null) {
                OpenSlideFFM.openslide_read_associated_image_icc_profile(
                        errorCtx.getOsr(), associated, data);
            } else {
                OpenSlideFFM.openslide_read_icc_profile(errorCtx.getOsr(),
                        data);
            }
        }
        try {
            return new ICC_ColorSpace(ICC_Profile.getInstance(data));
        } catch (IllegalArgumentException ex) {
            throw new IOException("Invalid ICC profile", ex);
        }
    }
}
