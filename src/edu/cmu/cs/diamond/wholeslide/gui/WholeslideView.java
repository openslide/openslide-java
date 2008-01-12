package edu.cmu.cs.diamond.wholeslide.gui;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import edu.cmu.cs.diamond.wholeslide.Wholeslide;

public class WholeslideView extends JComponent {
    private static final int BACKING_STORE_SIZE = 3;

    // allow a screenful on all sides
    final private double downsampleBase;

    final private int maxDownsampleExponent;

    transient final private Wholeslide wsd;

    transient private BufferedImage dbuf;

    private Point dbufOffset = new Point();

    private double rotation;

    private int downsampleExponent;

    private boolean firstPaint = true;

    private Point viewPosition = new Point();

    protected double tmpZoomScale = 1.0;

    protected int tmpZoomX;

    protected int tmpZoomY;

    private WholeslideView otherView;

    public WholeslideView(Wholeslide w) {
        this(w, 1.2, 40);
    }

    public WholeslideView(Wholeslide w, double downsampleBase,
            int maxDownsampleExponent) {
        wsd = w;
        this.downsampleBase = downsampleBase;
        this.maxDownsampleExponent = maxDownsampleExponent;

        setFocusable(true);
        setOpaque(true);

        registerEventHandlers();
    }

    @Override
    public void setBackground(Color bg) {
        super.setBackground(bg);
        redrawBackingStore();
        repaint();
    }

    static private void mouseReleasedHelper(WholeslideView w) {
        if (w == null) {
            return;
        }
        w.viewPosition.translate(w.dbufOffset.x, w.dbufOffset.y);
        w.redrawBackingStore(w.dbufOffset.x, w.dbufOffset.y);
        w.dbufOffset.move(0, 0);
    }

    static private void mouseDraggedHelper(WholeslideView w, int newX, int newY) {
        if (w == null) {
            return;
        }
        w.dbufOffset.move(newX, newY);
        w.repaint();
    }

    static private void spaceTyped(WholeslideView w) {
        if (w == null) {
            return;
        }
        Point delta = w.centerSlide();
        w.redrawBackingStore(delta.x, delta.y);
        w.repaint();
    }

    static private boolean mouseWheelHelper(WholeslideView w, MouseWheelEvent e) {
        if (w == null) {
            return false;
        }
        double origDS = w.getDownsample();
        w.zoomSlide(e.getX(), e.getY(), e.getWheelRotation());
        double relScale = origDS / w.getDownsample();

        w.tmpZoomX = e.getX();
        w.tmpZoomY = e.getY();
        w.tmpZoomScale = relScale;

        // TODO more fancy deferred zooming
        w.paintImmediately(0, 0, w.getWidth(), w.getHeight());

        w.tmpZoomScale = 1.0;
        return relScale != 1.0;
    }

    static private void mouseWheelHelper2(WholeslideView w) {
        if (w == null) {
            return;
        }
        w.redrawBackingStore();
        w.repaint();
    }

    private void registerEventHandlers() {
        // mouse wheel
        addMouseWheelListener(new MouseWheelListener() {
            public void mouseWheelMoved(MouseWheelEvent e) {
                boolean r1 = mouseWheelHelper(WholeslideView.this, e);
                boolean r2 = mouseWheelHelper(otherView, e);
                if (r1) {
                    mouseWheelHelper2(WholeslideView.this);
                }
                if (r2) {
                    mouseWheelHelper2(otherView);
                }
            }
        });

        // mouse drag
        MouseAdapter ma = new MouseAdapter() {
            private int x;

            private int y;

            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();

                if (!SwingUtilities.isLeftMouseButton(e)) {
                    return;
                }

                x = e.getX();
                y = e.getY();
                // System.out.println(dbufOffset);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) {
                    return;
                }

                mouseReleasedHelper(WholeslideView.this);
                mouseReleasedHelper(otherView);
                System.out.println(viewPosition);
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) {
                    return;
                }

                int newX = x - e.getX();
                int newY = y - e.getY();

                mouseDraggedHelper(WholeslideView.this, newX, newY);
                mouseDraggedHelper(otherView, newX, newY);
            }

        };
        addMouseListener(ma);
        addMouseMotionListener(ma);

        // keyboard
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                System.out.println(e);
                char key = e.getKeyChar();
                switch (key) {
                case ' ':
                    spaceTyped(WholeslideView.this);
                    spaceTyped(otherView);
                    break;
                }
            }
        });
    }

    protected void redrawBackingStore(int x, int y) {
        System.out.print("redrawing backing store with offset (" + x + "," + y
                + ")... ");
        System.out.flush();
        Graphics2D g = dbuf.createGraphics();
        g.setBackground(getBackground());

        int w = dbuf.getWidth();
        int h = dbuf.getHeight();

        int cw = w / BACKING_STORE_SIZE;
        int ch = h / BACKING_STORE_SIZE;

        double ds = getDownsample();

        // copy area
        g.copyArea(0, 0, w, h, -x, -y);

        // fill horiz
        if (y > 0) {
            // moved up, fill bottom
            g.clearRect(0, h - y, w, y);
            wsd.paintRegion(g, 0, h - y, viewPosition.x - cw, viewPosition.y
                    - ch + h - y, w, y, ds);

            // adjust h and y so as not to draw twice to the intersection
            h -= y;
            y = 0;
        } else if (y < 0) {
            // fill top
            g.clearRect(0, 0, w, -y);
            wsd.paintRegion(g, 0, 0, viewPosition.x - cw, viewPosition.y - ch,
                    w, -y, ds);

            // adjust h and y so as not to draw twice to the intersection
            h += y;
            y = -y;
        }

        // fill vert
        if (x > 0) {
            // fill right
            g.clearRect(w - x, y, x, h);
            wsd.paintRegion(g, w - x, y, viewPosition.x + w - x - cw,
                    viewPosition.y + y - ch, x, h, ds);
        } else if (x < 0) {
            // fill left
            g.clearRect(0, y, -x, h);
            wsd.paintRegion(g, 0, y, viewPosition.x - cw, viewPosition.y + y
                    - ch, -x, h, ds);
        }

        g.dispose();
        System.out.println("done");
    }

    private void zoomSlide(int mouseX, int mouseY, int amount) {
        double oldDS = getDownsample();

        int centerX = mouseX + viewPosition.x;
        int centerY = mouseY + viewPosition.y;

        final int bx = (int) (centerX * oldDS);
        final int by = (int) (centerY * oldDS);

        adjustDownsample(amount);

        final double newDS = getDownsample();

        viewPosition.translate((int) (bx / newDS) - centerX, (int) (by / newDS)
                - centerY);
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

    public Point centerSlide() {
        // TODO make faster drawing

        int w = getWidth();
        int h = getHeight();

        if (w == 0 || h == 0) {
            return new Point();
        }

        double ds = getDownsample();
        Dimension d = wsd.getLayer0Dimension();
        int dw = (int) (d.width / ds);
        int dh = (int) (d.height / ds);

        int newX = -(w / 2 - dw / 2);
        int newY = -(h / 2 - dh / 2);

        System.out.println("centering to " + newX + "," + newY);

        Point delta = new Point(newX - viewPosition.x, newY - viewPosition.y);
        viewPosition.move(newX, newY);

        return delta;
    }

    public void zoomToFit() {
        int w = getWidth();
        int h = getHeight();

        if (w == 0 || h == 0) {
            return;
        }

        Dimension d = wsd.getLayer0Dimension();
        double ws = (double) d.width / w;
        double hs = (double) d.height / h;

        double maxS = Math.max(ws, hs);

        if (maxS < 1.0) {
            downsampleExponent = 0;
        } else {
            downsampleExponent = (int) Math.ceil(Math.log(maxS)
                    / Math.log(downsampleBase));
        }

        if (downsampleExponent > maxDownsampleExponent) {
            downsampleExponent = maxDownsampleExponent;
        }

        System.out.println(downsampleExponent);
    }

    private void rotateSlide(double angle) {
        // TODO
    }

    private void setSlideRotation(double angle) {
        rotation = angle;
    }

    public void linkWithOther(WholeslideView otherView) {
        this.otherView = otherView;
        otherView.otherView = this;
    }

    public void unlinkOther() {
        if (otherView != null) {
            otherView.otherView = null;
            otherView = null;
        }
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
        // System.out.println(virtualBounds);
        return virtualBounds.getSize();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;

        Dimension sd = getScreenSize();
        int w = sd.width;
        int h = sd.height;

        if (firstPaint && w != 0 && h != 0) {
            zoomToFit();
            centerSlide();
            firstPaint = false;
        }

        possiblyInitBackingStore();

        AffineTransform a = new AffineTransform();
        a.translate(tmpZoomX, tmpZoomY);
        a.scale(tmpZoomScale, tmpZoomScale);
        a.translate(-tmpZoomX, -tmpZoomY);
        g2.transform(a);
        g2.drawImage(dbuf, -(dbufOffset.x + w), -(dbufOffset.y + h), null);
    }

    private void possiblyInitBackingStore() {
        // 3x so we can drag the entire length of the screen in all directions
        Dimension sd = getScreenSize();
        int w = sd.width * BACKING_STORE_SIZE;
        int h = sd.height * BACKING_STORE_SIZE;
        if (dbuf == null || dbuf.getWidth() != w || dbuf.getHeight() != h) {
            dbuf = getGraphicsConfiguration().createCompatibleImage(w, h,
                    Transparency.OPAQUE);
            System.out.println(dbuf);

            redrawBackingStore();
        }
    }

    private void redrawBackingStore() {
        long time = System.currentTimeMillis();
        System.out.print("redrawing backing store... ");
        System.out.flush();

        int w = dbuf.getWidth();
        int h = dbuf.getHeight();

        int cw = w / BACKING_STORE_SIZE;
        int ch = h / BACKING_STORE_SIZE;

        Graphics2D g = dbuf.createGraphics();
        g.setBackground(getBackground());
        g.clearRect(0, 0, w, h);

        // System.out.print(viewPosition.x + "," + viewPosition.y + " -> " + cw
        // + "," + ch + " ");
        // System.out.flush();

        wsd.paintRegion(g, 0, 0, viewPosition.x - cw, viewPosition.y - ch, w,
                h, getDownsample());
        g.dispose();
        
        long time2 = System.currentTimeMillis() - time;
        System.out.println("done (" + time2 + ")");
    }
}
