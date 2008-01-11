package edu.cmu.cs.diamond.wholeslide.gui;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import edu.cmu.cs.diamond.wholeslide.Wholeslide;

public class WholeslideView extends JComponent {
    private static final int BACKING_STORE_SIZE = 3;

    // allow a screenful on all sides
    final private double downsampleBase;

    final private int maxDownsampleExponent;

    final private Wholeslide wsd;

    final private static int DBUF_TYPE = BufferedImage.TYPE_INT_ARGB_PRE;

    private BufferedImage dbuf = new BufferedImage(1, 1, DBUF_TYPE);

    private Point dbufOffset = new Point();

    private double rotation;

    private int downsampleExponent;

    private boolean firstPaint = true;

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

    private void registerEventHandlers() {
        // mouse wheel
        addMouseWheelListener(new MouseWheelListener() {
            public void mouseWheelMoved(MouseWheelEvent e) {
                zoomSlide(e.getX(), e.getY(), e.getWheelRotation());

                redrawBackingStore();
                repaint();
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

                slidePosition.translate(dbufOffset.x, dbufOffset.y);
                redrawBackingStore(dbufOffset.x, dbufOffset.y);
                dbufOffset.move(0, 0);
                System.out.println(slidePosition);
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) {
                    return;
                }

                int newX = x - e.getX();
                int newY = y - e.getY();

                dbufOffset.move(newX, newY);
                // System.out.println(dbufOffset);

                repaint();
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
                    Point delta = centerSlide();
                    redrawBackingStore(delta.x, delta.y);
                    repaint();
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
            wsd.paintRegion(g, 0, h - y, slidePosition.x - cw, slidePosition.y
                    - ch + h - y, w, y, ds);

            // adjust h and y so as not to draw twice to the intersection
            h -= y;
            y = 0;
        } else if (y < 0) {
            // fill top
            g.clearRect(0, 0, w, -y);
            wsd.paintRegion(g, 0, 0, slidePosition.x - cw,
                    slidePosition.y - ch, w, -y, ds);

            // adjust h and y so as not to draw twice to the intersection
            h += y;
            y = -y;
        }

        // fill vert
        if (x > 0) {
            // fill right
            g.clearRect(w - x, y, x, h);
            wsd.paintRegion(g, w - x, y, slidePosition.x + w - x - cw,
                    slidePosition.y + y - ch, x, h, ds);
        } else if (x < 0) {
            // fill left
            g.clearRect(0, y, -x, h);
            wsd.paintRegion(g, 0, y, slidePosition.x - cw, slidePosition.y + y
                    - ch, -x, h, ds);
        }

        g.dispose();
        System.out.println("done");
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

        Point delta = new Point(newX - slidePosition.x, newY - slidePosition.y);
        slidePosition.move(newX, newY);

        return delta;
    }

    public void zoomToFit() {
        int w = getWidth();
        int h = getHeight();

        if (w == 0 || h == 0) {
            return;
        }

        Dimension d = wsd.getLayer0Dimension();
        double ws = d.width / w;
        double hs = d.height / h;

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
        // System.out.println(virtualBounds);
        return virtualBounds.getSize();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Dimension sd = getScreenSize();
        int w = sd.width;
        int h = sd.height;

        if (firstPaint && w != 0 && h != 0) {
            System.out.println("firstPaint");
            zoomToFit();
            centerSlide();
            firstPaint = false;
        }

        possiblyInitBackingStore();

        g.drawImage(dbuf, -(dbufOffset.x + w), -(dbufOffset.y + h), null);
    }

    private void possiblyInitBackingStore() {
        // 3x so we can drag the entire length of the screen in all directions
        Dimension sd = getScreenSize();
        int w = sd.width * BACKING_STORE_SIZE;
        int h = sd.height * BACKING_STORE_SIZE;
        if (dbuf.getWidth() != w || dbuf.getHeight() != h) {
            dbuf = getGraphicsConfiguration().createCompatibleImage(w, h,
                    Transparency.OPAQUE);
            System.out.println(dbuf);

            redrawBackingStore();
        }
    }

    private void redrawBackingStore() {
        System.out.print("redrawing backing store... ");
        System.out.flush();

        int w = dbuf.getWidth();
        int h = dbuf.getHeight();

        int cw = w / BACKING_STORE_SIZE;
        int ch = h / BACKING_STORE_SIZE;

        Graphics2D g = dbuf.createGraphics();
        g.setBackground(getBackground());
        g.clearRect(0, 0, w, h);

        System.out.print(slidePosition.x + "," + slidePosition.y + " -> " + cw
                + "," + ch + " ");
        System.out.flush();

        wsd.paintRegion(g, 0, 0, slidePosition.x - cw, slidePosition.y - ch, w,
                h, getDownsample());
        g.dispose();
        System.out.println("done");
    }
}
