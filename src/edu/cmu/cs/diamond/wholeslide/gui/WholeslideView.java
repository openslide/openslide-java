package edu.cmu.cs.diamond.wholeslide.gui;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import edu.cmu.cs.diamond.wholeslide.Wholeslide;

public class WholeslideView extends JComponent {
    final private double downsampleBase;

    final private int maxDownsampleExponent;

    final private Wholeslide wsd;

    final private static int DBUF_TYPE = BufferedImage.TYPE_INT_ARGB_PRE;

    private BufferedImage dbuf = new BufferedImage(1, 1, DBUF_TYPE);

    private Point dbufOffset = new Point();

    private double rotation;

    private int downsampleExponent = 10;

    // relative to centers of image and component
    private Point slidePosition = new Point();

    public WholeslideView(Wholeslide w) {
        this(w, 1.2, 40);
    }

    public WholeslideView(Wholeslide w, double downsampleBase,
            int maxDownsampleExponent) {
        wsd = w;
        this.downsampleBase = downsampleBase;
        this.maxDownsampleExponent = maxDownsampleExponent;

        setOpaque(true);

        registerEventHandlers();

        zoomToFit();
        centerSlide();

        // RepaintManager.currentManager(this).setDoubleBufferingEnabled(false);
    }

    private void registerEventHandlers() {
        // mouse wheel
        addMouseWheelListener(new MouseWheelListener() {
            public void mouseWheelMoved(MouseWheelEvent e) {
                zoomSlide(e.getX(), e.getY(), e.getWheelRotation());
            }
        });

        // mouse drag
        MouseAdapter ma = new MouseAdapter() {
            private int x;

            private int y;

            @Override
            public void mousePressed(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) {
                    return;
                }

                x = e.getX();
                y = e.getY();
                System.out.println(dbufOffset);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) {
                    return;
                }

                slidePosition.translate(dbufOffset.x, dbufOffset.y);
                dbufOffset.move(0, 0);
                redrawBackingStore();

                repaint();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) {
                    return;
                }

                int newX = x - e.getX();
                int newY = y - e.getY();

                dbufOffset.move(newX, newY);
                System.out.println(dbufOffset);

                repaint();
            }
        };
        addMouseListener(ma);
        addMouseMotionListener(ma);

        // keyboard
    }

    private void translateScaledSlide(int x, int y) {
        // TODO
    }

    private void zoomSlide(int centerX, int centerY, int amount) {
        System.out.println("amount: " + amount);
        double oldDS = getDownsample();

        final int bx = (int) (centerX * oldDS);
        final int by = (int) (centerY * oldDS);

        adjustDownsample(amount);

        final double newDS = getDownsample();
        System.out.println(newDS);

        slidePosition.move((int) (bx / newDS) - centerX, (int) (by / newDS)
                - centerY);

        redrawBackingStore();
        repaint();
    }

    private void adjustDownsample(int amount) {
        downsampleExponent += amount;

        if (downsampleExponent < 0) {
            downsampleExponent = 0;
        } else if (downsampleExponent > maxDownsampleExponent) {
            downsampleExponent = maxDownsampleExponent;
        }
    }

    private double getDownsample() {
        return Math.pow(downsampleBase, downsampleExponent);
    }

    public void centerSlide() {
        slidePosition.move(0, 0);
    }

    public void zoomToFit() {
        // TODO
    }

    private void rotateSlide(double angle) {
        // TODO
    }

    private void setSlideRotation(double angle) {
        rotation = angle;
    }

    public void linkWithOther(WholeslideView otherView) {
        // TODO
    }

    public void unlinkOther() {
        // TODO
    }

    Dimension getScreenSize() {
        // from javadoc example
        Rectangle virtualBounds = new Rectangle();
        GraphicsEnvironment ge = GraphicsEnvironment
                .getLocalGraphicsEnvironment();
        GraphicsDevice[] gs = ge.getScreenDevices();
        for (int j = 0; j < gs.length; j++) {
            GraphicsDevice gd = gs[j];
            GraphicsConfiguration[] gc = gd.getConfigurations();
            for (int i = 0; i < gc.length; i++) {
                virtualBounds = virtualBounds.union(gc[i].getBounds());
            }
        }
        System.out.println(virtualBounds);
        return virtualBounds.getSize();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Dimension sd = getScreenSize();
        int w = sd.width;
        int h = sd.height;

        possiblyInitBackingStore();

        g.drawImage(dbuf, -(dbufOffset.x + w), -(dbufOffset.y + h), null);
    }

    private void possiblyInitBackingStore() {
        // TODO optimize based on location on screen (3x screen size not
        // required)
        Dimension sd = getScreenSize();
        int w = sd.width * 3;
        int h = sd.height * 3;
        if (dbuf.getWidth() != w || dbuf.getHeight() != h) {
            dbuf = getGraphicsConfiguration().createCompatibleImage(w, h,
                    Transparency.OPAQUE);
            System.out.println(dbuf);

            redrawBackingStore();
        }
    }

    private void redrawBackingStore() {
        // TODO don't redraw the entire thing, use copyarea, etc.
        System.out.print("redrawing backing store... ");
        System.out.flush();

        Dimension sd = getScreenSize();
        int w = sd.width * 3;
        int h = sd.height * 3;

        Graphics2D g = dbuf.createGraphics();
        wsd.paintRegion(g, 0, 0, slidePosition.x, slidePosition.y, w, h,
                getDownsample());
        g.dispose();
        System.out.println("done");
    }
}
