package edu.cmu.cs.diamond.wholeslide.gui;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import edu.cmu.cs.diamond.wholeslide.Wholeslide;

public class WholeslideView extends JComponent {
    private static final int TILE_SIZE = 256;

    final private double downsampleBase;

    final private int maxDownsampleExponent;

    transient final private Wholeslide wsd;

    private double rotation;

    private int downsampleExponent;

    private boolean firstPaint = true;

    private Point viewPosition = new Point();

    protected double tmpZoomScale = 1.0;

    protected int tmpZoomX;

    protected int tmpZoomY;

    private WholeslideView otherView;

    final private Map<Point, BufferedImage> tiles = Collections
            .synchronizedMap(new HashMap<Point, BufferedImage>());

    final private BlockingQueue<Point> dirtyTiles = new LinkedBlockingQueue<Point>();

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

        final int otX = (oX / TILE_SIZE) * TILE_SIZE;
        final int otY = (oY / TILE_SIZE) * TILE_SIZE;

        Rectangle bounds = new Rectangle(oX, oY, w, h);

        Rectangle tmpTile = new Rectangle(TILE_SIZE, TILE_SIZE);

        // remove
        Iterator<Point> it = tiles.keySet().iterator();
        while (it.hasNext()) {
            Point p = it.next();
            tmpTile.x = p.x;
            tmpTile.y = p.y;
            if (!bounds.intersects(tmpTile)) {
                it.remove();
            }
        }

        // add
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

    private void addNewTile(Point p) {
        System.out.println("adding new tile for " + p);

        BufferedImage b = getGraphicsConfiguration().createCompatibleImage(
                TILE_SIZE, TILE_SIZE, Transparency.OPAQUE);
        Graphics2D g = b.createGraphics();
        g.setBackground(getBackground());
        g.setColor(Color.BLACK);
        g.clearRect(0, 0, TILE_SIZE, TILE_SIZE);
        g.drawRect(0, 0, TILE_SIZE, TILE_SIZE);
        g.drawString(p.toString(), 10, 10);
        g.dispose();

        tiles.put(p, b);

        try {
            dirtyTiles.put(p);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    static private void spaceTyped(WholeslideView w) {
        if (w == null) {
            return;
        }
        w.centerSlide();
        w.addRemoveTiles();
        w.repaint();
    }

    static private void mouseWheelHelper(WholeslideView w, MouseWheelEvent e) {
        if (w == null) {
            return;
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
        w.addRemoveTiles();
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

        paintAllTiles(g);
    }

    private void paintAllTiles(Graphics g) {
        Dimension sd = getScreenSize();
        int h = getHeight();
        int w = getWidth();

        System.out.println("drawing from " + viewPosition);

        int startX = viewPosition.x;
        int startY = viewPosition.y;
        int extraX = (startX + sd.width) % TILE_SIZE;
        int extraY = (startY + sd.height) % TILE_SIZE;
        
        Point p = new Point();
        for (int y = startY; y < h + startY + extraY; y += TILE_SIZE) {
            int ty = ((y + sd.height) / TILE_SIZE) * TILE_SIZE;
            for (int x = startX; x < w + startX + extraX; x += TILE_SIZE) {
                int tx = ((x + sd.width) / TILE_SIZE) * TILE_SIZE;

                p.move(tx, ty);
                BufferedImage b = tiles.get(p);
                System.out.println("draw " + p + " " + (b != null));
                g.drawImage(b, x - startX - extraX, y - startY - extraY, null);
            }
        }
    }
}
