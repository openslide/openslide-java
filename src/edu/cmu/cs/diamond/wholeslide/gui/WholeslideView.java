package edu.cmu.cs.diamond.wholeslide.gui;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import edu.cmu.cs.diamond.wholeslide.Wholeslide;

public class WholeslideView extends JComponent {
    private static final int KEYBOARD_SCROLL_AMOUNT = 100;

    final private double downsampleBase;

    final private int maxDownsampleExponent;

    transient final private Wholeslide wsd;

    private double rotation;

    private int downsampleExponent;

    private boolean firstPaint = true;

    private Point viewPosition = new Point();

    private WholeslideView otherView;

    protected boolean makingSelection;

    protected Rectangle selection;

    transient private BufferedImage dbuf;

    private double tmpZoomScale = 1.0;

    private int tmpZoomX;

    private int tmpZoomY;

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
        paintBackingStore();
        repaint();
    }

    static private void translateHelper(WholeslideView ws, int dX, int dY) {
        if (ws == null) {
            return;
        }
        ws.translateSlide(dX, dY);
    }

    static private void repaintHelper(WholeslideView w) {
        if (w == null) {
            return;
        }

        w.repaint();
    }

    static private void centerHelper(WholeslideView w) {
        if (w == null) {
            return;
        }
        w.centerSlide();
        w.repaint();
    }

    private void translateSlide(int dX, int dY) {
        int w = dbuf.getWidth();
        int h = dbuf.getHeight();

        Graphics2D g = dbuf.createGraphics();
        g.copyArea(0, 0, w, h, -dX, -dY);
        viewPosition.translate(dX, dY);

        if (dY > 0) {
            // moved up, fill bottom
            g.setClip(0, h - dY, w, dY);
            paintBackingStore(g);

            // adjust h and y so as not to draw twice to the intersection
            h -= dY;
            dY = 0;
        } else if (dY < 0) {
            // fill top
            g.setClip(0, 0, w, -dY);
            paintBackingStore(g);

            // adjust h and y so as not to draw twice to the intersection
            h += dY;
            dY = -dY;
        }

        // fill vert
        if (dX > 0) {
            // fill right
            g.setClip(w - dX, dY, dX, h);
            paintBackingStore(g);
        } else if (dX < 0) {
            // fill left
            g.setClip(0, dY, -dX, h);
            paintBackingStore(g);
        }
        g.dispose();
    }

    static private double zoomHelper(WholeslideView w, MouseWheelEvent e) {
        if (w == null) {
            return 1.0;
        }

        double oldDS = w.getDownsample();
        w.zoomSlide(e.getX(), e.getY(), e.getWheelRotation());

        double newDS = w.getDownsample();

        return oldDS / newDS;
    }

    static private void zoomHelper2(WholeslideView w, double relDS,
            MouseWheelEvent e) {
        if (w == null) {
            return;
        }

        if (relDS != 1.0) {
            w.tmpZoomScale = relDS;
            w.tmpZoomX = e.getX();
            w.tmpZoomY = e.getY();

            w.paintImmediately(0, 0, w.getWidth(), w.getHeight());

            w.tmpZoomScale = 1.0;
            w.tmpZoomX = 0;
            w.tmpZoomY = 0;
        }
    }

    static private void zoomHelper3(WholeslideView w, double relDS) {
        if (w == null) {
            return;
        }

        if (relDS != 1.0) {
            w.paintBackingStore();
        }
    }

    private void registerEventHandlers() {
        // mouse wheel
        addMouseWheelListener(new MouseWheelListener() {
            public void mouseWheelMoved(MouseWheelEvent e) {
                double ds1 = zoomHelper(WholeslideView.this, e);
                double ds2 = zoomHelper(otherView, e);
                zoomHelper2(WholeslideView.this, ds1, e);
                zoomHelper2(otherView, ds2, e);
                zoomHelper3(WholeslideView.this, ds1);
                zoomHelper3(otherView, ds2);
                repaintHelper(WholeslideView.this);
                repaintHelper(otherView);
            }
        });

        // mouse drag
        MouseAdapter ma = new MouseAdapter() {
            private int oldX;

            private int oldY;

            private int slideStartX;

            private int slideStartY;

            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();

                if (!SwingUtilities.isLeftMouseButton(e)) {
                    return;
                }

                makingSelection = (e.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) == MouseEvent.SHIFT_DOWN_MASK;

                oldX = e.getX();
                oldY = e.getY();

                double ds = getDownsample();
                slideStartX = (int) ((oldX + viewPosition.x) * ds);
                slideStartY = (int) ((oldY + viewPosition.y) * ds);
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) {
                    return;
                }

                int relX = oldX - e.getX();
                int relY = oldY - e.getY();

                if (!makingSelection) {
                    translateHelper(WholeslideView.this, relX, relY);
                    translateHelper(otherView, relX, relY);
                    repaintHelper(WholeslideView.this);
                    repaintHelper(otherView);
                } else {
                    double ds = getDownsample();
                    int dx = slideStartX;
                    int dy = slideStartY;
                    int dw = (int) ((e.getX() + viewPosition.x) * ds) - dx;
                    int dh = (int) ((e.getY() + viewPosition.y) * ds) - dy;

                    if (dw < 0) {
                        dx += dw;
                        dw = -dw;
                    }
                    if (dh < 0) {
                        dy += dh;
                        dh = -dh;
                    }

                    selection = new Rectangle(dx, dy, dw, dh);
                    // System.out.println(selection);
                    repaint();
                }
                oldX = e.getX();
                oldY = e.getY();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                makingSelection = false;
            }

        };
        addMouseListener(ma);
        addMouseMotionListener(ma);

        // keyboard
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                System.out.println(e);
                int key = e.getKeyCode();
                switch (key) {
                case KeyEvent.VK_SPACE:
                    centerHelper(WholeslideView.this);
                    centerHelper(otherView);
                    repaintHelper(WholeslideView.this);
                    repaintHelper(otherView);
                    break;
                case KeyEvent.VK_UP:
                case KeyEvent.VK_W:
                    translateHelper(WholeslideView.this, 0,
                            -KEYBOARD_SCROLL_AMOUNT);
                    translateHelper(otherView, 0, -KEYBOARD_SCROLL_AMOUNT);
                    repaintHelper(WholeslideView.this);
                    repaintHelper(otherView);
                    break;
                case KeyEvent.VK_DOWN:
                case KeyEvent.VK_S:
                    translateHelper(WholeslideView.this, 0,
                            KEYBOARD_SCROLL_AMOUNT);
                    translateHelper(otherView, 0, KEYBOARD_SCROLL_AMOUNT);
                    repaintHelper(WholeslideView.this);
                    repaintHelper(otherView);
                    break;
                case KeyEvent.VK_LEFT:
                case KeyEvent.VK_A:
                    translateHelper(WholeslideView.this,
                            -KEYBOARD_SCROLL_AMOUNT, 0);
                    translateHelper(otherView, -KEYBOARD_SCROLL_AMOUNT, 0);
                    repaintHelper(WholeslideView.this);
                    repaintHelper(otherView);
                    break;
                case KeyEvent.VK_RIGHT:
                case KeyEvent.VK_D:
                    translateHelper(WholeslideView.this,
                            KEYBOARD_SCROLL_AMOUNT, 0);
                    translateHelper(otherView, KEYBOARD_SCROLL_AMOUNT, 0);
                    repaintHelper(WholeslideView.this);
                    repaintHelper(otherView);
                    break;
                case KeyEvent.VK_ESCAPE:
                    selection = null;
                    repaint();
                    break;
                case KeyEvent.VK_ENTER:
                    paintBackingStore();
                    repaint();
                    break;
                }
            }
        });
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

    public void centerSlide() {
        int w = getWidth();
        int h = getHeight();

        if (w == 0 || h == 0) {
            return;
        }

        double ds = getDownsample();
        Dimension d = wsd.getLayer0Dimension();
        int dw = (int) (d.width / ds);
        int dh = (int) (d.height / ds);

        int newX = -(w / 2 - dw / 2);
        int newY = -(h / 2 - dh / 2);

        translateSlide(newX - viewPosition.x, newY - viewPosition.y);
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

    @Override
    protected void paintComponent(Graphics g) {
        int w = getWidth();
        int h = getHeight();
        if (firstPaint) {
            if (w != 0 && h != 0) {
                createBackingStore();
                zoomToFit();
                centerSlide();
                paintBackingStore();
                firstPaint = false;
            } else {
                return;
            }
        }

        Graphics2D g2 = (Graphics2D) g;

        if (dbuf.getWidth() != w || dbuf.getHeight() != h) {
            createBackingStore();
            paintBackingStore();
        }

        AffineTransform a = g2.getTransform();
        if (tmpZoomScale != 1.0) {
            g2.setBackground(getBackground());
            g2.clearRect(0, 0, w, h);
            g2.translate(tmpZoomX, tmpZoomY);
            g2.scale(tmpZoomScale, tmpZoomScale);
            g2.translate(-tmpZoomX, -tmpZoomY);
        }
        g2.drawImage(dbuf, 0, 0, null);
        g2.setTransform(a);
        paintSelection(g2);
    }

    private void createBackingStore() {
        dbuf = getGraphicsConfiguration().createCompatibleImage(getWidth(),
                getHeight(), Transparency.OPAQUE);
    }

    private void paintBackingStore() {
        Graphics2D dg = dbuf.createGraphics();
        dg.setClip(0, 0, dbuf.getWidth(), dbuf.getHeight());
        paintBackingStore(dg);
        dg.dispose();
    }

    private void paintBackingStore(Graphics2D g) {
        double ds = getDownsample();
        int offsetX = viewPosition.x;
        int offsetY = viewPosition.y;

        Rectangle clip = g.getClipBounds();

        g.setBackground(getBackground());
        g.clearRect(clip.x, clip.y, clip.width, clip.height);

        wsd.paintRegion(g, clip.x, clip.y, offsetX + clip.x, offsetY + clip.y,
                clip.width, clip.height, ds);
    }

    private void paintSelection(Graphics2D g) {
        if (selection != null) {
            double ds = getDownsample();

            int x = (int) (selection.x / ds - viewPosition.x);
            int y = (int) (selection.y / ds - viewPosition.y);
            int w = (int) Math.round(selection.width / ds);
            int h = (int) Math.round(selection.height / ds);

            g.setColor(new Color(1.0f, 0.0f, 0.0f, 0.15f));
            g.fillRect(x, y, w, h);
            g.setColor(Color.RED);
            g.drawRect(x, y, w, h);
        }
    }
}
