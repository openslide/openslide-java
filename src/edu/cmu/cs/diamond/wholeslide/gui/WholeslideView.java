package edu.cmu.cs.diamond.wholeslide.gui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import javax.swing.*;

import edu.cmu.cs.diamond.wholeslide.Wholeslide;

public class WholeslideView extends JComponent {
    private static final int KEYBOARD_SCROLL_AMOUNT = 100;

    final private double downsampleBase;

    final private int maxDownsampleExponent;

    transient final private Wholeslide wsd;

    private int rotation;

    private int downsampleExponent;

    private boolean firstPaint = true;

    private Point viewPosition = new Point();

    private WholeslideView otherView;

    protected Shape selection;

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
        if (dbuf != null) {
            paintBackingStore();
            repaint();
        }
    }

    static private void translateHelper(WholeslideView ws, int dX, int dY) {
        if (ws == null) {
            return;
        }
        ws.translateSlidePrivate(dX, dY);
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
        w.centerSlidePrivate();
    }

    private void translateSlidePrivate(int dX, int dY) {
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

    static private double zoomHelper(WholeslideView w, int x, int y, int amount) {
        if (w == null) {
            return 1.0;
        }

        double oldDS = w.getDownsample();
        w.zoomSlide(x, y, amount);

        double newDS = w.getDownsample();

        return oldDS / newDS;
    }

    static private void zoomHelper2(WholeslideView w, double relDS, int x, int y) {
        if (w == null) {
            return;
        }

        if (relDS != 1.0) {
            w.tmpZoomScale = relDS;
            w.tmpZoomX = x;
            w.tmpZoomY = y;

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
                double ds1 = zoomHelper(WholeslideView.this, e.getX(),
                        e.getY(), e.getWheelRotation());
                double ds2 = zoomHelper(otherView, e.getX(), e.getY(), e
                        .getWheelRotation());
                zoomHelper2(WholeslideView.this, ds1, e.getX(), e.getY());
                zoomHelper2(otherView, ds2, e.getX(), e.getY());
                zoomHelper3(WholeslideView.this, ds1);
                zoomHelper3(otherView, ds2);
                repaintHelper(WholeslideView.this);
                repaintHelper(otherView);
            }
        });

        // mouse drag
        MouseAdapter ma = new MouseAdapter() {
            final private static int SELECTION_MODE_NONE = 0;

            final private static int SELECTION_MODE_RECT = 1;

            final private static int SELECTION_MODE_FREEHAND = 2;

            private int selectionMode;

            private int oldX;

            private int oldY;

            private int slideStartX;

            private int slideStartY;

            private GeneralPath freehandPath;

            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();

                if (!SwingUtilities.isLeftMouseButton(e)) {
                    return;
                }

                if ((e.getModifiersEx() & MouseEvent.CTRL_DOWN_MASK) == MouseEvent.CTRL_DOWN_MASK) {
                    selectionMode = SELECTION_MODE_FREEHAND;
                    selection = null;
                } else if ((e.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) == MouseEvent.SHIFT_DOWN_MASK) {
                    selectionMode = SELECTION_MODE_RECT;
                    selection = null;
                } else {
                    selectionMode = SELECTION_MODE_NONE;
                }

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

                double ds;
                int dw;
                int dh;

                switch (selectionMode) {
                case SELECTION_MODE_NONE:
                    translateHelper(WholeslideView.this, relX, relY);
                    translateHelper(otherView, relX, relY);
                    repaintHelper(WholeslideView.this);
                    repaintHelper(otherView);
                    break;

                case SELECTION_MODE_RECT:
                    ds = getDownsample();
                    int dx = slideStartX;
                    int dy = slideStartY;
                    dw = (int) ((e.getX() + viewPosition.x) * ds) - dx;
                    dh = (int) ((e.getY() + viewPosition.y) * ds) - dy;

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
                    break;

                case SELECTION_MODE_FREEHAND:
                    if (selection == null) {
                        // new selection
                        freehandPath = new GeneralPath();
                        selection = freehandPath;

                        freehandPath.moveTo(slideStartX, slideStartY);
                    }

                    ds = getDownsample();
                    dx = (int) ((e.getX() + viewPosition.x) * ds);
                    dy = (int) ((e.getY() + viewPosition.y) * ds);

                    freehandPath.lineTo(dx, dy);

                    repaint();
                    break;
                }
                oldX = e.getX();
                oldY = e.getY();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (selectionMode == SELECTION_MODE_FREEHAND) {
                    freehandPath.closePath();
                }
                selectionMode = SELECTION_MODE_NONE;

                if (selection != null) {
                    Rectangle bb = selection.getBounds();
                    if (bb.height == 0 || bb.width == 0) {
                        selection = null;
                    }
                }
                repaint();
            }

        };
        addMouseListener(ma);
        addMouseMotionListener(ma);

        // keyboard
        InputMap inputMap = new InputMap();
        ActionMap actionMap = new ActionMap();

        inputMap.put(KeyStroke.getKeyStroke("SPACE"), "center");
        actionMap.put("center", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                centerHelper(WholeslideView.this);
                centerHelper(otherView);
                repaintHelper(WholeslideView.this);
                repaintHelper(otherView);
            }
        });

        inputMap.put(KeyStroke.getKeyStroke("UP"), "scroll up");
        inputMap.put(KeyStroke.getKeyStroke("W"), "scroll up");
        actionMap.put("scroll up", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                translateHelper(WholeslideView.this, 0, -KEYBOARD_SCROLL_AMOUNT);
                translateHelper(otherView, 0, -KEYBOARD_SCROLL_AMOUNT);
                repaintHelper(WholeslideView.this);
                repaintHelper(otherView);
            }
        });

        inputMap.put(KeyStroke.getKeyStroke("DOWN"), "scroll down");
        inputMap.put(KeyStroke.getKeyStroke("S"), "scroll down");
        actionMap.put("scroll down", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                translateHelper(WholeslideView.this, 0, KEYBOARD_SCROLL_AMOUNT);
                translateHelper(otherView, 0, KEYBOARD_SCROLL_AMOUNT);
                repaintHelper(WholeslideView.this);
                repaintHelper(otherView);
            }
        });

        inputMap.put(KeyStroke.getKeyStroke("LEFT"), "scroll left");
        inputMap.put(KeyStroke.getKeyStroke("A"), "scroll left");
        actionMap.put("scroll left", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                translateHelper(WholeslideView.this, -KEYBOARD_SCROLL_AMOUNT, 0);
                translateHelper(otherView, -KEYBOARD_SCROLL_AMOUNT, 0);
                repaintHelper(WholeslideView.this);
                repaintHelper(otherView);
            }
        });

        inputMap.put(KeyStroke.getKeyStroke("RIGHT"), "scroll right");
        inputMap.put(KeyStroke.getKeyStroke("D"), "scroll right");
        actionMap.put("scroll right", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                translateHelper(WholeslideView.this, KEYBOARD_SCROLL_AMOUNT, 0);
                translateHelper(otherView, KEYBOARD_SCROLL_AMOUNT, 0);
                repaintHelper(WholeslideView.this);
                repaintHelper(otherView);
            }
        });

        inputMap.put(KeyStroke.getKeyStroke("ESCAPE"), "clear selection");
        actionMap.put("clear selection", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                selection = null;
                repaint();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke("L"), "rotate left");
        actionMap.put("rotate left", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {

            }
        });

        inputMap.put(KeyStroke.getKeyStroke("R"), "rotate right");
        actionMap.put("rotate right", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {

            }
        });

        inputMap.put(KeyStroke.getKeyStroke("PLUS"), "zoom in");
        inputMap.put(KeyStroke.getKeyStroke("EQUALS"), "zoom in");
        actionMap.put("zoom in", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                double d1 = zoomHelper(WholeslideView.this, -1);
                double d2 = zoomHelper(otherView, -1);
                zoomHelper2(WholeslideView.this, d1);
                zoomHelper2(otherView, d2);
                zoomHelper3(WholeslideView.this, d1);
                zoomHelper3(otherView, d2);
                repaintHelper(WholeslideView.this);
                repaintHelper(otherView);
            }
        });

        inputMap.put(KeyStroke.getKeyStroke("MINUS"), "zoom out");
        actionMap.put("zoom out", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                double d1 = zoomHelper(WholeslideView.this, 1);
                double d2 = zoomHelper(otherView, 1);
                zoomHelper2(WholeslideView.this, d1);
                zoomHelper2(otherView, d2);
                zoomHelper3(WholeslideView.this, d1);
                zoomHelper3(otherView, d2);
                repaintHelper(WholeslideView.this);
                repaintHelper(otherView);
            }
        });

        // install as parents
        InputMap oldInputMap = getInputMap();
        ActionMap oldActionMap = getActionMap();
        inputMap.setParent(oldInputMap.getParent());
        oldInputMap.setParent(inputMap);
        actionMap.setParent(oldActionMap.getParent());
        oldActionMap.setParent(actionMap);
    }

    protected void zoomHelper2(WholeslideView w, double d) {
        if (w == null) {
            return;
        }
        zoomHelper2(w, d, w.getWidth() / 2, w.getHeight() / 2);
    }

    static protected double zoomHelper(WholeslideView w, int i) {
        if (w == null) {
            return 1.0;
        }

        return zoomHelper(w, w.getWidth() / 2, w.getHeight() / 2, i);
    }

    private void zoomSlide(int mouseX, int mouseY, int amount) {
        double oldDS = getDownsample();

        int centerX = mouseX + viewPosition.x;
        int centerY = mouseY + viewPosition.y;

        final double bx = centerX * oldDS;
        final double by = centerY * oldDS;

        adjustDownsample(amount);

        final double newDS = getDownsample();

        if (oldDS != newDS) {
            viewPosition.translate((int) Math.round(bx / newDS) - centerX,
                    (int) Math.round(by / newDS) - centerY);
        }
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
        centerSlidePrivate();
        repaint();
    }

    public void setViewPosition(int x, int y) {
        translateSlidePrivate(x - viewPosition.x, y - viewPosition.y);
        repaint();
    }

    private void centerSlidePrivate() {
        Insets insets = getInsets();

        int w = getWidth() - insets.left - insets.right;
        int h = getHeight() - insets.top - insets.bottom;

        if (w <= 0 || h <= 0) {
            return;
        }

        double ds = getDownsample();
        Dimension d = wsd.getLayer0Dimension();
        int dw = (int) (d.width / ds);
        int dh = (int) (d.height / ds);

        int centerX = w / 2 + insets.left;
        int centerY = h / 2 + insets.top;

        int centerDX = dw / 2;
        int centerDY = dh / 2;

        int newX = -(centerX - centerDX);
        int newY = -(centerY - centerDY);

        translateSlidePrivate(newX - viewPosition.x, newY - viewPosition.y);
    }

    private void zoomToFit() {
        Insets insets = getInsets();

        int w = getWidth() - insets.left - insets.right;
        int h = getHeight() - insets.top - insets.bottom;

        if (w <= 0 || h <= 0) {
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

        // System.out.println(downsampleExponent);
    }

    private void rotateSlide(int quads) {
        rotation += quads;
        rotation %= 4;
        if (rotation < 0) {
            rotation += 4;
        }
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
        Insets insets = getInsets();

        int w = getWidth();
        int h = getHeight();

        if (firstPaint) {
            if (w != 0 && h != 0) {
                createBackingStore();
                zoomToFit();
                centerSlidePrivate();
                paintBackingStore();
                firstPaint = false;
            } else {
                return;
            }
        }

        if (dbuf.getWidth() != w || dbuf.getHeight() != h) {
            createBackingStore();
            paintBackingStore();
        }

        Graphics scratchG = g.create();
        Graphics2D g2 = (Graphics2D) scratchG;
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            g2.clipRect(insets.left, insets.top,
                    w - insets.left - insets.right, h - insets.top
                            - insets.bottom);

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
        } finally {
            scratchG.dispose();
        }
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

    public static void paintSelection(Graphics2D g, Shape selection, int x,
            int y, double downsample) {
        AffineTransform at = new AffineTransform();
        at.translate(x, y);
        at.scale(1 / downsample, 1 / downsample);

        Shape s = at.createTransformedShape(selection);

        Rectangle2D bb = s.getBounds2D();

        float strokeWidth = 5;
        double bw = bb.getWidth();
        double bh = bb.getHeight();

        if (bw < 6 && bh < 6) {
            s = new Rectangle((int) bb.getCenterX() - 2,
                    (int) bb.getCenterY() - 2, 4, 4);
        }

        if (bw < 10 || bh < 10) {
            strokeWidth = 3;
        }

        g.setStroke(new BasicStroke(strokeWidth));
        g.setColor(Color.BLACK);
        g.draw(s);

        g.setStroke(new BasicStroke(strokeWidth - 2, BasicStroke.CAP_SQUARE,
                BasicStroke.JOIN_BEVEL, 0, new float[] { 3, 9 }, 0f));
        // g.setColor(Color.BLACK);
        g.setColor(new Color(176, 255, 107));
        g.draw(s);

        g.setStroke(new BasicStroke(strokeWidth - 2, BasicStroke.CAP_SQUARE,
                BasicStroke.JOIN_BEVEL, 0, new float[] { 3, 9 }, 6f));
        // g.setColor(Color.YELLOW);
        g.setColor(new Color(107, 176, 255));
        g.draw(s);
    }

    private void paintSelection(Graphics2D g) {
        if (selection == null) {
            return;
        }

        paintSelection(g, selection, -viewPosition.x, -viewPosition.y,
                getDownsample());
    }

    public Shape getSelection() {
        return selection;
    }

    public void setSelection(Shape s) {
        selection = s;
        repaint();
    }
    
    public Wholeslide getWholeslide() {
        return wsd;
    }
}
