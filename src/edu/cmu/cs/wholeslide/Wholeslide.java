package edu.cmu.cs.wholeslide;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;

import edu.cmu.cs.wholeslide.glue.SWIGTYPE_p__wholeslide;

public class Wholeslide {
    private SWIGTYPE_p__wholeslide wsd;

    final private long layerWidths[];

    final private long layerHeights[];

    final private int layerCount;

    public static boolean fileIsValid(File file) {
        return edu.cmu.cs.wholeslide.glue.Wholeslide
                .ws_can_open(file.getPath());
    }

    public Wholeslide(File file) {
        wsd = edu.cmu.cs.wholeslide.glue.Wholeslide.ws_open(file.getPath());

        if (wsd == null) {
            // TODO not just file not found
            throw new WholeslideException();
        }

        // store layer count
        layerCount = edu.cmu.cs.wholeslide.glue.Wholeslide
                .ws_get_layer_count(wsd);

        // store dimensions
        layerWidths = new long[layerCount];
        layerHeights = new long[layerCount];

        for (int i = 0; i < layerCount; i++) {
            long w[] = new long[1];
            long h[] = new long[1];
            edu.cmu.cs.wholeslide.glue.Wholeslide.ws_get_layer_dimensions(wsd,
                    i, w, h);
            layerWidths[i] = w[0];
            layerHeights[i] = h[0];
        }
    }

    public void dispose() {
        if (wsd != null) {
            edu.cmu.cs.wholeslide.glue.Wholeslide.ws_close(wsd);
            wsd = null;
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
        if (wsd == null) {
            throw new WholeslideDisposedException();
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

        return edu.cmu.cs.wholeslide.glue.Wholeslide.ws_get_comment(wsd);
    }

    public void paintRegion(Graphics2D g, int dx, int dy, int sx, int sy,
            int w, int h, double downsample) {
        checkDisposed();

        if (downsample < 1.0) {
            throw new IllegalArgumentException("downsample (" + downsample
                    + ") must be >= 1.0");
        }

        // get the layer
        int layer = edu.cmu.cs.wholeslide.glue.Wholeslide
                .ws_get_best_layer_for_downsample(wsd, downsample);

        // figure out its downsample
        double layerDS = edu.cmu.cs.wholeslide.glue.Wholeslide
                .ws_get_layer_downsample(wsd, layer);

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

        edu.cmu.cs.wholeslide.glue.Wholeslide.ws_read_region(wsd, data, baseX,
                baseY, layer, img.getWidth(), img.getHeight());

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
            int maxSize) {
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

        BufferedImage result = new BufferedImage(sw, sh,
                BufferedImage.TYPE_INT_ARGB_PRE);

        Graphics2D g = result.createGraphics();
        paintRegion(g, 0, 0, sx, sy, sw, sh, ds);
        g.dispose();
        return result;
    }

    public BufferedImage createThumbnailImage(int maxSize) {
        return createThumbnailImage(0, 0, getLayer0Width(), getLayer0Height(),
                maxSize);
    }
}
