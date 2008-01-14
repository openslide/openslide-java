package edu.cmu.cs.diamond.wholeslide.gui;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import edu.cmu.cs.diamond.wholeslide.Wholeslide;

public class WholeslideView extends JComponent {
    private static final int KEYBOARD_SCROLL_AMOUNT = 100;

    private static final int TILE_SIZE = 256;

    final private double downsampleBase;

    final private int maxDownsampleExponent;

    transient final private Wholeslide wsd;

    private double rotation;

    private int downsampleExponent;

    private boolean firstPaint = true;

    private Point viewPosition = new Point();

    private static final Point poisonPoint = new Point();

    private WholeslideView otherView;

    // final private Map<Point, BufferedImage> tiles = new TreeMap<Point,
    // BufferedImage>(
    // new Comparator<Point>() {
    // public int compare(Point o1, Point o2) {
    // int yc = Integer.valueOf(o1.y).compareTo(
    // Integer.valueOf(o2.y));
    // if (yc != 0) {
    // return yc;
    // } else {
    // return Integer.valueOf(o1.x).compareTo(
    // Integer.valueOf(o2.x));
    // }
    // }
    // });
    final private Map<Point, BufferedImage> tiles = new HashMap<Point, BufferedImage>();

    final private BlockingQueue<Point> dirtyTiles = new LinkedBlockingQueue<Point>();

    @Override
    protected void finalize() throws Throwable {
        dirtyTiles.put(poisonPoint);
    }

    protected Runnable redrawer = new Runnable() {
        public void run() {
            repaint();
        }
    };

    protected boolean makingSelection;

    protected Rectangle selection;

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

        startDrawingThread();
    }

    private void startDrawingThread() {
        new Thread(new Runnable() {
            public void run() {
                while (true) {
                    try {
                        Point p = dirtyTiles.take();
                        if (p == poisonPoint) {
                            return; // done
                        }

                        // get the tile
                        System.out.println("checking " + p);

                        BufferedImage tile;
                        synchronized (tiles) {
                            if (!tiles.containsKey(p)) {
                                continue;
                            }
                            tile = tiles.get(p);
                        }
                        drawTileForPoint(p, tile);

                        SwingUtilities.invokeLater(redrawer);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    protected void drawTileForPoint(Point p, BufferedImage b) {
        Graphics2D g = b.createGraphics();
        System.out.println("drawing tile for point " + p);
        double ds = getDownsample();
        g.setBackground(getBackground());
        g.clearRect(0, 0, TILE_SIZE, TILE_SIZE);
        wsd.paintRegion(g, 0, 0, p.x, p.y, TILE_SIZE, TILE_SIZE, ds);
        // g.setColor(Color.BLACK);
        // g.drawString(p.toString(), 10, 10);
        g.dispose();
    }

    private void drawEmptyTile(BufferedImage emptyTile) {
        Graphics2D g = emptyTile.createGraphics();
        g.setBackground(getBackground());
        g.clearRect(0, 0, TILE_SIZE, TILE_SIZE);
        g.setColor(Color.RED);
        g.drawLine(0, 0, TILE_SIZE, TILE_SIZE);
        g.drawLine(0, TILE_SIZE, TILE_SIZE, 0);
        g.drawRect(0, 0, TILE_SIZE - 1, TILE_SIZE - 1);
        g.dispose();
    }

    @Override
    public void setBackground(Color bg) {
        super.setBackground(bg);
        repaint();
    }

    static private void mouseDraggedHelper(WholeslideView w, int newX, int newY) {
        if (w == null) {
            return;
        }
        w.viewPosition.move(newX, newY);
        System.out.println(w.viewPosition);
        w.addRemoveTiles();
        w.repaint();
    }

    private void addRemoveTiles() {
        Dimension sd = getScreenSize();
        final int w = sd.width * 3;
        final int h = sd.height * 3;

        final int oX = viewPosition.x - sd.width;
        final int oY = viewPosition.y - sd.height;

        System.out.println("oX: " + oX + ", oY: " + oY);

        final int otX = getTileAt(oX);
        final int otY = getTileAt(oY);

        Rectangle bounds = new Rectangle(oX, oY, w, h);

        Rectangle tmpTile = new Rectangle(TILE_SIZE, TILE_SIZE);

        synchronized (tiles) {
            // remove
            Iterator<Point> it = tiles.keySet().iterator();
            while (it.hasNext()) {
                Point p = it.next();
                tmpTile.x = p.x;
                tmpTile.y = p.y;
                if (!bounds.intersects(tmpTile)) {
                    it.remove();
                    // dirtyTiles.remove(p);
                }
            }
        }

        // add
        addTiles(sd.width, sd.height, getTileAt(oX + sd.width), getTileAt(oY
                + sd.height), bounds);
        addTiles(w, h, otX, otY, bounds);

        /*
         * // add, in a spiral int maxD = Math.max(w, h); int maxSteps = maxD /
         * TILE_SIZE + ((maxD % TILE_SIZE != 0) ? 1 : 0); maxSteps *= maxSteps;
         * int y = getTileAt(h / 2); int x = getTileAt(w / 2); int dir = 0; int
         * steps = 1; int stepsUntilChangeDir = steps * 2; for (int i = 0; i <
         * maxSteps; i++) { tmpTile.setLocation(otX + x, otY + y); if
         * (bounds.intersects(tmpTile)) { Point p = tmpTile.getLocation(); if
         * (!tiles.containsKey(p)) { addNewTile(p); } }
         * 
         * switch (dir) { case 0: x += TILE_SIZE; break; case 1: y += TILE_SIZE;
         * break; case 2: x -= TILE_SIZE; break; case 3: y -= TILE_SIZE; break; }
         * stepsUntilChangeDir--; if (stepsUntilChangeDir == 0) { if (dir == 1 ||
         * dir == 3) { steps++; } dir = (dir + 1) % 4; stepsUntilChangeDir =
         * steps; } }
         */
    }

    private void addTiles(int w, int h, int otX, int otY, Rectangle bounds) {
        Rectangle tmpTile = new Rectangle(TILE_SIZE, TILE_SIZE);
        for (int y = 0; y < h; y += TILE_SIZE) {
            for (int x = 0; x < w; x += TILE_SIZE) {
                tmpTile.setLocation(otX + x, otY + y);
                if (bounds.intersects(tmpTile)) {
                    Point p = tmpTile.getLocation();
                    if (!tiles.containsKey(p)) {
                        addNewTile(p);
                    }
                }
            }
        }
    }

    static private int getTileAt(int p) {
        if (p < 0) {
            int p2 = (p / TILE_SIZE) * TILE_SIZE;
            if (p == p2) {
                return p;
            } else {
                return p2 - TILE_SIZE;
            }
        } else {
            return (p / TILE_SIZE) * TILE_SIZE;
        }
    }

    private void addNewTile(Point p) {
        // System.out.println("adding new tile for " + p);
        BufferedImage emptyTile = createTile();
        drawEmptyTile(emptyTile);
        synchronized (tiles) {
            tiles.put(p, emptyTile);
        }

        try {
            System.out.println("adding " + p + " to dirtyTiles");
            dirtyTiles.put(p);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private BufferedImage createTile() {
        BufferedImage b = getGraphicsConfiguration().createCompatibleImage(
                TILE_SIZE, TILE_SIZE, Transparency.OPAQUE);
        return b;
    }

    static private void spaceTyped(WholeslideView w) {
        if (w == null) {
            return;
        }
        w.centerSlide();
        w.addRemoveTiles();
        w.repaint();
    }

    static private void translateSlide(WholeslideView w, int x, int y) {
        if (w == null) {
            return;
        }

        w.viewPosition.translate(x, y);
        w.addRemoveTiles();
        w.repaint();
    }

    static private void mouseWheelHelper(WholeslideView w, MouseWheelEvent e) {
        if (w == null) {
            return;
        }
        w.zoomSlide(e.getX(), e.getY(), e.getWheelRotation());
        w.removeAllTiles();
        w.addRemoveTiles();
    }

    private void removeAllTiles() {
        synchronized (tiles) {
            tiles.clear();
        }
    }

    private void dirtyAllTiles() {
        synchronized (tiles) {
            dirtyTiles.clear();
            for (Point p : tiles.keySet()) {
                try {
                    dirtyTiles.put(p);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void registerEventHandlers() {
        // mouse wheel
        addMouseWheelListener(new MouseWheelListener() {
            public void mouseWheelMoved(MouseWheelEvent e) {
                mouseWheelHelper(WholeslideView.this, e);
                mouseWheelHelper(otherView, e);
            }
        });

        // mouse drag
        MouseAdapter ma = new MouseAdapter() {
            private int x;

            private int y;

            private int viewX;

            private int viewY;

            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();

                if (!SwingUtilities.isLeftMouseButton(e)) {
                    return;
                }

                makingSelection = (e.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) == MouseEvent.SHIFT_DOWN_MASK;

                x = e.getX();
                y = e.getY();
                viewX = viewPosition.x;
                viewY = viewPosition.y;
                // System.out.println(dbufOffset);
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) {
                    return;
                }

                int newX = viewX + x - e.getX();
                int newY = viewY + y - e.getY();

                if (!makingSelection) {
                    mouseDraggedHelper(WholeslideView.this, newX, newY);
                    mouseDraggedHelper(otherView, newX, newY);
                } else {
                    double ds = getDownsample();
                    int dx = (int) ((viewX + x) * ds);
                    int dy = (int) ((viewY + y) * ds);
                    int dw = (int) ((e.getX() - x) * ds);
                    int dh = (int) ((e.getY() - y) * ds);

                    if (dw < 0) {
                        dx += dw;
                        dw = -dw;
                    }
                    if (dh < 0) {
                        dy += dh;
                        dh = -dh;
                    }

                    selection = new Rectangle(dx, dy, dw, dh);
                    repaint();
                }
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
                    spaceTyped(WholeslideView.this);
                    spaceTyped(otherView);
                    break;
                case KeyEvent.VK_UP:
                case KeyEvent.VK_W:
                    translateSlide(WholeslideView.this, 0,
                            -KEYBOARD_SCROLL_AMOUNT);
                    break;
                case KeyEvent.VK_DOWN:
                case KeyEvent.VK_S:
                    translateSlide(WholeslideView.this, 0,
                            KEYBOARD_SCROLL_AMOUNT);
                    break;
                case KeyEvent.VK_LEFT:
                case KeyEvent.VK_A:
                    translateSlide(WholeslideView.this,
                            -KEYBOARD_SCROLL_AMOUNT, 0);
                    break;
                case KeyEvent.VK_RIGHT:
                case KeyEvent.VK_D:
                    translateSlide(WholeslideView.this, KEYBOARD_SCROLL_AMOUNT,
                            0);
                    break;
                case KeyEvent.VK_ESCAPE:
                    selection = null;
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

        System.out.println("centering to " + newX + "," + newY);

        viewPosition.move(newX, newY);
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
        if (firstPaint) {
            Dimension sd = getScreenSize();
            int w = sd.width;
            int h = sd.height;

            if (w != 0 && h != 0) {
                zoomToFit();
                centerSlide();
                addRemoveTiles();
                firstPaint = false;
            }
        }

        Graphics2D g2 = (Graphics2D) g;

        paintAllTiles(g2);
        paintSelection(g2);
    }

    private void paintSelection(Graphics2D g) {
        if (selection != null) {
            double ds = getDownsample();

            g.translate(-viewPosition.x, -viewPosition.y);
            g.scale(1 / ds, 1 / ds);
            g.setColor(new Color(1.0f, 0.0f, 0.0f, 0.15f));
            g.fill(selection);
            g.setColor(Color.RED);
            g.draw(selection);
        }
    }

    static private int getExtraAt(int p) {
        if (p < 0) {
            return -(getTileAt(p) - p);
        } else {
            return p % TILE_SIZE;
        }
    }

    private void paintAllTiles(Graphics2D g) {
        Dimension sd = getScreenSize();
        int h = getHeight();
        int w = getWidth();

        int sOffsetX = sd.width;
        int sOffsetY = sd.height;

        // System.out.println("drawing from " + viewPosition);

        int startX = viewPosition.x - sOffsetX;
        int startY = viewPosition.y - sOffsetY;
        int extraX = getExtraAt(viewPosition.x);
        int extraY = getExtraAt(viewPosition.y);

        System.out.println("extra: " + extraX + "," + extraY);

        Point p = new Point();
        for (int y = startY; y < h + startY + extraY; y += TILE_SIZE) {
            int ty = getTileAt(y + sOffsetY);
            for (int x = startX; x < w + startX + extraX; x += TILE_SIZE) {
                int tx = getTileAt(x + sOffsetX);

                p.move(tx, ty);

                BufferedImage b;
                synchronized (tiles) {
                    b = tiles.get(p);
                }

                int dx = x - startX - extraX;
                int dy = y - startY - extraY;
                // System.out.println("draw " + p + " " + (b != null) + " -> "
                // + dx + "," + dy);
                g.drawImage(b, dx, dy, null);
            }
        }
    }
}
