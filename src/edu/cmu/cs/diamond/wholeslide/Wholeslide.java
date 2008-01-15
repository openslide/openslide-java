package edu.cmu.cs.diamond.wholeslide;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;

import edu.cmu.cs.diamond.wholeslide.glue.SWIGTYPE_p__wholeslide;

public class Wholeslide {
    private SWIGTYPE_p__wholeslide wsd;

    final private int baselineW;

    final private int baselineH;

    public static boolean fileIsValid(File file) {
        return edu.cmu.cs.diamond.wholeslide.glue.Wholeslide.ws_can_open(file
                .getPath());
    }

    public Wholeslide(File file) {
        wsd = edu.cmu.cs.diamond.wholeslide.glue.Wholeslide.ws_open(file
                .getPath());

        if (wsd == null) {
            // TODO not just file not found
            throw new WholeslideException();
        }

        // store baseline
        int w[] = new int[1];
        int h[] = new int[1];
        edu.cmu.cs.diamond.wholeslide.glue.Wholeslide.ws_get_layer0_dimensions(
                wsd, w, h);
        baselineW = w[0];
        baselineH = h[0];
    }

    public void dispose() {
        if (wsd != null) {
            edu.cmu.cs.diamond.wholeslide.glue.Wholeslide.ws_close(wsd);
            wsd = null;
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        dispose();
    }

    public int getLayerCount() {
        checkDisposed();

        return edu.cmu.cs.diamond.wholeslide.glue.Wholeslide
                .ws_get_layer_count(wsd);
    }

    private void checkDisposed() {
        if (wsd == null) {
            throw new WholeslideDisposedException();
        }
    }

    public Dimension getLayer0Dimension() {
        checkDisposed();

        return new Dimension(baselineW, baselineH);
    }

    public Dimension getLayerDimension(int layer) {
        checkDisposed();

        int[] x = new int[1];
        int[] y = new int[1];
        edu.cmu.cs.diamond.wholeslide.glue.Wholeslide.ws_get_layer_dimensions(
                wsd, layer, x, y);

        return new Dimension(x[0], y[0]);
    }

    public String getComment() {
        checkDisposed();

        return edu.cmu.cs.diamond.wholeslide.glue.Wholeslide
                .ws_get_comment(wsd);
    }

    public void paintRegion(Graphics2D g, int dx, int dy, int sx, int sy,
            int w, int h, double downsample) {
        checkDisposed();

        if (downsample < 1.0) {
            throw new IllegalArgumentException("downsample (" + downsample
                    + ") must be >= 1.0");
        }

        // get the layer
        int layer = edu.cmu.cs.diamond.wholeslide.glue.Wholeslide
                .ws_get_best_layer_for_downsample(wsd, downsample);

        // figure out its downsample
        double layerDS = edu.cmu.cs.diamond.wholeslide.glue.Wholeslide
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
        Dimension d = getLayerDimension(layer);
        layerW = Math.min(layerW, d.width - layerX);
        layerH = Math.min(layerH, d.height - layerY);
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

        edu.cmu.cs.diamond.wholeslide.glue.Wholeslide.ws_read_region(wsd, data,
                baseX, baseY, layer, img.getWidth(), img.getHeight());

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

    final boolean debug = true;

    int debugThingy = 0;
}
