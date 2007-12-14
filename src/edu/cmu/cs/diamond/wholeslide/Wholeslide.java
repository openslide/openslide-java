package edu.cmu.cs.diamond.wholeslide;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;

import edu.cmu.cs.diamond.wholeslide.glue.SWIGTYPE_p__wholeslide;

public class Wholeslide {
    final private SWIGTYPE_p__wholeslide wsd;

    public Wholeslide(File file) {
        wsd = edu.cmu.cs.diamond.wholeslide.glue.Wholeslide.ws_open(file
                .getPath());
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        edu.cmu.cs.diamond.wholeslide.glue.Wholeslide.ws_close(wsd);
    }

    public int getLayerCount() {
        return edu.cmu.cs.diamond.wholeslide.glue.Wholeslide
                .ws_get_layer_count(wsd);
    }

    public Dimension getBaselineDimension() {
        int x[] = new int[1];
        int y[] = new int[1];
        edu.cmu.cs.diamond.wholeslide.glue.Wholeslide
                .ws_get_baseline_dimensions(wsd, x, y);

        return new Dimension(x[0], y[0]);
    }

    public Dimension getLayerDimension(int layer) {
        int[] x = new int[1];
        int[] y = new int[1];
        edu.cmu.cs.diamond.wholeslide.glue.Wholeslide.ws_get_layer_dimensions(
                wsd, layer, x, y);

        return new Dimension(x[0], y[0]);
    }

    public String getComment() {
        return edu.cmu.cs.diamond.wholeslide.glue.Wholeslide
                .ws_get_comment(wsd);
    }

    public void paintRegion(Graphics2D g, int dx, int dy, int sx, int sy,
            int w, int h, double downsample) {
        int layer = edu.cmu.cs.diamond.wholeslide.glue.Wholeslide
                .ws_get_best_layer_for_downsample(wsd, downsample);

        double actualDownsample = edu.cmu.cs.diamond.wholeslide.glue.Wholeslide
                .ws_get_layer_downsample(wsd, layer);

        double newDownsample = downsample / actualDownsample;

        g.setColor(getBackgroundColor());
        g.fillRect(dx, dy, w, h);

        System.out.println("newDownsample " + newDownsample);

        getLayerDimension(layer);
        
        int newW = (int) (newDownsample * w);
        int newH = (int) (newDownsample * h);
        int newX = (int) (downsample * sx);
        int newY = (int) (downsample * sy);

        System.out.println("newW " + newW + ", newH " + newH + ", newX " + newX
                + ", newY " + newY);

        BufferedImage img = new BufferedImage(newW, newH,
                BufferedImage.TYPE_INT_ARGB_PRE);

        int data[] = ((DataBufferInt) img.getRaster().getDataBuffer())
                .getData();

        edu.cmu.cs.diamond.wholeslide.glue.Wholeslide.ws_read_region(wsd, data,
                newX, newY, layer, newW, newH);

        g.drawImage(img, dx, dy, w, h, null);
    }

    private Color getBackgroundColor() {
        int color = edu.cmu.cs.diamond.wholeslide.glue.Wholeslide
                .ws_get_background_color(wsd);
        return new Color((color >> 16) & 0xFF, (color >> 8) & 0xFF,
                (color >> 0) & 0xFF, (color >> 24) & 0xFF);
    }
}
