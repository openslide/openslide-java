/*
 *  OpenSlide, a library for reading whole slide image files
 *
 *  Copyright (c) 2007-2010 Carnegie Mellon University
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

/*
 * This should be rewritten. Problems include: very clunky handling of linked
 * slides, non-pixel-perfect scrolling.
 */

package org.openslide.gui;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.swing.*;

import org.openslide.OpenSlide;

public class OpenSlideView extends JPanel {
    private static final int KEYBOARD_SCROLL_AMOUNT = 100;

    private boolean selectionsAsPins;

    final private double downsampleBase;

    final private int maxDownsampleExponent;

    transient final private OpenSlide osr;

    private int rotation;

    private int downsampleExponent;

    private boolean firstPaint = true;

    private Point viewPosition = new Point();

    private OpenSlideView otherView;

    private SelectionListModel selections = new DefaultSelectionListModel();

    private Shape selectionBeingDrawn;

    transient private BufferedImage dbuf;

    private double tmpZoomScale = 1.0;

    private int tmpZoomX;

    private int tmpZoomY;

    private final boolean startWithZoomFit;

    private boolean selectionsVisible;

    public OpenSlideView(OpenSlide w) {
        this(w, false);
    }

    public OpenSlideView(OpenSlide w, boolean startWithZoomFit) {
        this(w, 1.2, 40, startWithZoomFit);
    }

    public OpenSlideView(OpenSlide w, double downsampleBase,
            int maxDownsampleExponent, boolean startWithZoomFit) {
        // TODO support w > 2^31 and h > 2^31
        if (w.getLevel0Width() > Integer.MAX_VALUE
                || w.getLevel0Height() > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                    "OpenSlide size must not exceed (" + Integer.MAX_VALUE
                            + "," + Integer.MAX_VALUE + ")");
        }

        osr = w;
        this.downsampleBase = downsampleBase;
        this.maxDownsampleExponent = maxDownsampleExponent;
        this.startWithZoomFit = startWithZoomFit;

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

    static private void translateHelper(OpenSlideView ws, int dX, int dY) {
        if (ws == null) {
            return;
        }
        ws.translateSlidePrivate(dX, dY);
    }

    static private void repaintHelper(OpenSlideView w) {
        if (w == null) {
            return;
        }

        w.repaint();
    }

    static private void selectionsVisibleHelper(OpenSlideView w, boolean visible) {
        if (w == null) {
            return;
        }

        w.selectionsVisible = visible;
        w.repaint();
    }

    static private void centerHelper(OpenSlideView w) {
        if (w == null) {
            return;
        }
        w.centerSlidePrivate();
    }

    private void translateSlidePrivate(int dX, int dY) {
        int w = dbuf.getWidth();
        int h = dbuf.getHeight();
        Graphics2D g = dbuf.createGraphics();

        if (Math.abs(dX) >= w || Math.abs(dY) >= h) {
            viewPosition.translate(dX, dY);
            g.setClip(0, 0, w, h);
            paintBackingStore(g);
            g.dispose();
            return;
        }

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

    static private double zoomHelper(OpenSlideView w, int x, int y, int amount) {
        if (w == null) {
            return 1.0;
        }

        double oldDS = w.getDownsample();
        w.zoomSlide(x, y, amount);

        double newDS = w.getDownsample();

        return oldDS / newDS;
    }

    static private void zoomHelper2(OpenSlideView w, double relDS, int x, int y) {
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

    static private void zoomHelper3(OpenSlideView w, double relDS) {
        if (w == null) {
            return;
        }

        if (relDS != 1.0) {
            w.paintBackingStore();
        }
    }

    private enum SelectionMode {
        NONE, RECT, FREEHAND, ELLIPSE;
    }

    private void registerEventHandlers() {
        // mouse wheel
        addMouseWheelListener(new MouseWheelListener() {
            public void mouseWheelMoved(MouseWheelEvent e) {
                double ds1 = zoomHelper(OpenSlideView.this, e.getX(), e.getY(),
                        e.getWheelRotation());
                double ds2 = zoomHelper(otherView, e.getX(), e.getY(), e
                        .getWheelRotation());
                zoomHelper2(OpenSlideView.this, ds1, e.getX(), e.getY());
                zoomHelper2(otherView, ds2, e.getX(), e.getY());
                zoomHelper3(OpenSlideView.this, ds1);
                zoomHelper3(otherView, ds2);
                repaintHelper(OpenSlideView.this);
                repaintHelper(otherView);
            }
        });

        // mouse drag
        MouseAdapter ma = new MouseAdapter() {
            private SelectionMode selectionMode;

            private int oldX;

            private int oldY;

            private int slideStartX;

            private int slideStartY;

            private Path2D.Double freehandPath;

            @Override
            public void mousePressed(MouseEvent e) {
                // System.out.println(e);

                requestFocusInWindow();

                if (!SwingUtilities.isLeftMouseButton(e)) {
                    return;
                }

                final int ellipseMask = MouseEvent.CTRL_DOWN_MASK
                        | MouseEvent.SHIFT_DOWN_MASK;
                final int freehandMask = MouseEvent.CTRL_DOWN_MASK;
                final int rectMask = MouseEvent.SHIFT_DOWN_MASK;

                if ((e.getModifiersEx() & ellipseMask) == ellipseMask) {
                    selectionMode = SelectionMode.ELLIPSE;
                } else if ((e.getModifiersEx() & freehandMask) == freehandMask) {
                    selectionMode = SelectionMode.FREEHAND;
                } else if ((e.getModifiersEx() & rectMask) == rectMask) {
                    selectionMode = SelectionMode.RECT;
                } else {
                    selectionMode = SelectionMode.NONE;
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

                switch (selectionMode) {
                case NONE:
                    translateHelper(OpenSlideView.this, relX, relY);
                    translateHelper(otherView, relX, relY);
                    repaintHelper(OpenSlideView.this);
                    repaintHelper(otherView);
                    break;

                case RECT:
                    selectionBeingDrawn = new Rectangle(dx, dy, dw, dh);
                    // System.out.println(selection);
                    repaint();
                    break;

                case FREEHAND:
                    if (selectionBeingDrawn == null) {
                        // new selection
                        freehandPath = new Path2D.Double();
                        selectionBeingDrawn = freehandPath;

                        freehandPath.moveTo(slideStartX, slideStartY);
                    }

                    freehandPath.lineTo((e.getX() + viewPosition.x) * ds, (e
                            .getY() + viewPosition.y)
                            * ds);

                    repaint();
                    break;

                case ELLIPSE:
                    selectionBeingDrawn = new Ellipse2D.Double(dx, dy, dw, dh);

                    repaint();
                    break;
                }
                oldX = e.getX();
                oldY = e.getY();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (selectionMode == SelectionMode.FREEHAND) {
                    freehandPath.closePath();
                }
                selectionMode = SelectionMode.NONE;

                if (selectionBeingDrawn != null) {
                    Rectangle bb = selectionBeingDrawn.getBounds();
                    if (bb.height != 0 && bb.width != 0) {
                        selections.add(new DefaultAnnotation(selectionBeingDrawn));
                        selectionBeingDrawn = null;
                    }
                }
                repaint();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                selectionsVisibleHelper(OpenSlideView.this, true);
                selectionsVisibleHelper(otherView, true);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                selectionsVisibleHelper(OpenSlideView.this, false);
                selectionsVisibleHelper(otherView, false);
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
                centerHelper(OpenSlideView.this);
                centerHelper(otherView);
                repaintHelper(OpenSlideView.this);
                repaintHelper(otherView);
            }
        });

        inputMap.put(KeyStroke.getKeyStroke("UP"), "scroll up");
        inputMap.put(KeyStroke.getKeyStroke("W"), "scroll up");
        actionMap.put("scroll up", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                translateHelper(OpenSlideView.this, 0, -KEYBOARD_SCROLL_AMOUNT);
                translateHelper(otherView, 0, -KEYBOARD_SCROLL_AMOUNT);
                repaintHelper(OpenSlideView.this);
                repaintHelper(otherView);
            }
        });

        inputMap.put(KeyStroke.getKeyStroke("DOWN"), "scroll down");
        inputMap.put(KeyStroke.getKeyStroke("S"), "scroll down");
        actionMap.put("scroll down", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                translateHelper(OpenSlideView.this, 0, KEYBOARD_SCROLL_AMOUNT);
                translateHelper(otherView, 0, KEYBOARD_SCROLL_AMOUNT);
                repaintHelper(OpenSlideView.this);
                repaintHelper(otherView);
            }
        });

        inputMap.put(KeyStroke.getKeyStroke("LEFT"), "scroll left");
        inputMap.put(KeyStroke.getKeyStroke("A"), "scroll left");
        actionMap.put("scroll left", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                translateHelper(OpenSlideView.this, -KEYBOARD_SCROLL_AMOUNT, 0);
                translateHelper(otherView, -KEYBOARD_SCROLL_AMOUNT, 0);
                repaintHelper(OpenSlideView.this);
                repaintHelper(otherView);
            }
        });

        inputMap.put(KeyStroke.getKeyStroke("RIGHT"), "scroll right");
        inputMap.put(KeyStroke.getKeyStroke("D"), "scroll right");
        actionMap.put("scroll right", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                translateHelper(OpenSlideView.this, KEYBOARD_SCROLL_AMOUNT, 0);
                translateHelper(otherView, KEYBOARD_SCROLL_AMOUNT, 0);
                repaintHelper(OpenSlideView.this);
                repaintHelper(otherView);
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
                double d1 = zoomHelper(OpenSlideView.this, -1);
                double d2 = zoomHelper(otherView, -1);
                zoomHelper2(OpenSlideView.this, d1);
                zoomHelper2(otherView, d2);
                zoomHelper3(OpenSlideView.this, d1);
                zoomHelper3(otherView, d2);
                repaintHelper(OpenSlideView.this);
                repaintHelper(otherView);
            }
        });

        inputMap.put(KeyStroke.getKeyStroke("MINUS"), "zoom out");
        actionMap.put("zoom out", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                double d1 = zoomHelper(OpenSlideView.this, 1);
                double d2 = zoomHelper(otherView, 1);
                zoomHelper2(OpenSlideView.this, d1);
                zoomHelper2(otherView, d2);
                zoomHelper3(OpenSlideView.this, d1);
                zoomHelper3(otherView, d2);
                repaintHelper(OpenSlideView.this);
                repaintHelper(otherView);
            }
        });

        inputMap.put(KeyStroke.getKeyStroke("Z"), "zoom to fit");
        actionMap.put("zoom to fit", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                // System.out.println("zoom");
                zoomToFit();
                centerSlidePrivate();
                paintBackingStore();
                repaint();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke("1"), "zoom to 1:1");
        actionMap.put("zoom to 1:1", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                // System.out.println("zoom 1:1");
                double d1 = zoomHelper(OpenSlideView.this, Integer.MIN_VALUE);
                double d2 = zoomHelper(otherView, Integer.MIN_VALUE);
                zoomHelper2(OpenSlideView.this, d1);
                zoomHelper2(otherView, d2);
                zoomHelper3(OpenSlideView.this, d1);
                zoomHelper3(otherView, d2);
                repaintHelper(OpenSlideView.this);
                repaintHelper(otherView);
            }
        });

        inputMap.put(KeyStroke.getKeyStroke("BACK_QUOTE"), "toggle pins");
        actionMap.put("toggle pins", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                selectionsAsPins = !selectionsAsPins;
                if (otherView != null) {
                    otherView.selectionsAsPins = !otherView.selectionsAsPins;
                }
                repaintHelper(OpenSlideView.this);
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

    protected void zoomHelper2(OpenSlideView w, double d) {
        if (w == null) {
            return;
        }
        zoomHelper2(w, d, w.getWidth() / 2, w.getHeight() / 2);
    }

    static protected double zoomHelper(OpenSlideView w, int i) {
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

    public void centerOnSelection(int selection) {
        centerOnSelectionPrivate(selection);
        repaint();
    }

    private void centerOnSelectionPrivate(int s) {
        if (selections.isEmpty() || s == -1) {
            centerSlidePrivate();
        } else {
            Shape selection = selections.get(s).getShape();
            Rectangle2D bb = selection.getBounds2D();
            centerSlidePrivate((int) bb.getCenterX(), (int) bb.getCenterY());
        }
    }

    private void centerSlidePrivate() {
        centerSlidePrivate((int) (osr.getLevel0Width() / 2), (int) (osr
                .getLevel0Height() / 2));
    }

    private void centerSlidePrivate(int cX, int cY) {
        Insets insets = getInsets();

        int w = getWidth() - insets.left - insets.right;
        int h = getHeight() - insets.top - insets.bottom;

        if (w <= 0 || h <= 0) {
            return;
        }

        int centerX = w / 2 + insets.left;
        int centerY = h / 2 + insets.top;

        double ds = getDownsample();
        int centerDX = (int) (cX / ds);
        int centerDY = (int) (cY / ds);

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

        double ws = (double) osr.getLevel0Width() / w;
        double hs = (double) osr.getLevel0Height() / h;

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

    public void linkWithOther(OpenSlideView otherView) {
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
        super.paintComponent(g);

        Insets insets = getInsets();

        int w = getWidth();
        int h = getHeight();

        if (firstPaint) {
            if (w != 0 && h != 0) {
                createBackingStore();
                if (startWithZoomFit) {
                    zoomToFit();
                }

                centerOnSelectionPrivate(0);

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
                    RenderingHints.VALUE_ANTIALIAS_OFF);

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
            if (selectionsVisible) {
                g2.setComposite(AlphaComposite.SrcOver);
            } else {
                g2.setComposite(AlphaComposite.getInstance(
                        AlphaComposite.SRC_OVER, 0.2f));
            }
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

        try {
            osr.paintRegion(g, clip.x, clip.y, offsetX + clip.x, offsetY
                    + clip.y, clip.width, clip.height, ds);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void paintSelection(Graphics2D g, Shape selection, int x,
            int y, double downsample) {
        paintSelection(g, selection, x, y, downsample, false);
    }

    public static void paintSelection(Graphics2D g, Shape selection, int x,
            int y, double downsample, boolean asPins) {
        AffineTransform at = new AffineTransform();
        at.translate(x, y);
        at.scale(1 / downsample, 1 / downsample);

        Shape s = at.createTransformedShape(selection);

        if (asPins) {
            Rectangle2D r = s.getBounds2D();
            int sx = (int) Math.round(r.getCenterX());
            int sy = (int) Math.round(r.getCenterY());
            g.setColor(Color.RED);
            g.fillRect(sx - 2, sy - 2, 4, 4);
        } else {
            g.translate(1, 1);
            g.setColor(Color.BLACK);
            g.draw(s);

            g.translate(-1, -1);
            g.setColor(Color.WHITE);
            g.draw(s);
        }
    }

    private void paintSelection(Graphics2D g) {
        if (selectionBeingDrawn != null) {
            paintSelection(g, selectionBeingDrawn, -viewPosition.x,
                    -viewPosition.y, getDownsample(), false);
        }
        for (Annotation selection : selections) {
            paintSelection(g, selection.getShape(), -viewPosition.x,
                    -viewPosition.y, getDownsample(), selectionsAsPins);
        }
    }

    public void addSelection(Shape s) {
        selections.add(new DefaultAnnotation(s));
        repaint();
    }

    public OpenSlide getOpenSlide() {
        return osr;
    }

    public long getSlideX(int x) {
        return (long) ((viewPosition.x + x) * getDownsample());
    }

    public long getSlideY(int y) {
        return (long) ((viewPosition.y + y) * getDownsample());
    }

    public SelectionListModel getSelectionListModel() {
        return selections;
    }

    public void setSelectionListModel(SelectionListModel selections) {
        this.selections = selections;
    }

    public int getSelectionForPoint(int x, int y) {
        for (int i = 0; i < selections.getSize(); i++) {
            Shape s = selections.get(i).getShape();
            if (s.contains(new Point(x, y))) {
                return i;
            }
        }
        return -1;
    }
}
