package edu.cmu.cs.diamond.wholeslide;

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

        int layer = edu.cmu.cs.diamond.wholeslide.glue.Wholeslide
                .ws_get_best_layer_for_downsample(wsd, downsample);

        double actualDownsample = edu.cmu.cs.diamond.wholeslide.glue.Wholeslide
                .ws_get_layer_downsample(wsd, layer);

        double newDownsample = downsample / actualDownsample;

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

        // System.out.println("newDownsample " + newDownsample);

        int newW = (int) (newDownsample * w);
        int newH = (int) (newDownsample * h);
        int newX = (int) (downsample * sx);
        int newY = (int) (downsample * sy);

        Dimension d = getLayerDimension(layer);

        if (newW > d.width - sx) {
            newW = d.width - sx;
            w = (int) (newW / newDownsample);
        }
        if (newH > d.height - sy) {
            newH = d.height - sy;
            h = (int) (newH / newDownsample);
        }

        if (newW <= 0 || newH <= 0) {
            // nothing to draw
            return;
        }

        // System.out.println("newW " + newW + ", newH " + newH + ", newX " +
        // newX
        // + ", newY " + newY);

        BufferedImage img = new BufferedImage(newW, newH,
                BufferedImage.TYPE_INT_ARGB_PRE);

        int data[] = ((DataBufferInt) img.getRaster().getDataBuffer())
                .getData();

        edu.cmu.cs.diamond.wholeslide.glue.Wholeslide.ws_read_region(wsd, data,
                newX, newY, layer, img.getWidth(), img.getHeight());

        g.drawImage(img, dx, dy, w, h, null);
    }
}
