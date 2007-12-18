package edu.cmu.cs.diamond.wholeslide;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.io.File;

import javax.swing.*;

public class Test extends JPanel {

    private static final int MIN_SIZE = 100;

    final transient private Wholeslide wsd;

    private int downsampleFactor = 0;

    final private int maxDownsampleFactor;

    private static final double DOWNSAMPLE_BASE = 1.2;

    private void adjustDownsample(int amount) {
        downsampleFactor += amount;

        if (downsampleFactor < 0) {
            downsampleFactor = 0;
        } else if (downsampleFactor > maxDownsampleFactor) {
            downsampleFactor = maxDownsampleFactor;
        }

        updateSize();
    }

    private double getDownsample() {
        return Math.pow(DOWNSAMPLE_BASE, downsampleFactor);
    }

    public Test(Wholeslide w) {
        wsd = w;

        setBackground(wsd.getBackgroundColor());

        Dimension d = wsd.getBaselineDimension();
        maxDownsampleFactor = (int) Math.max(Math.log(d.getHeight() / MIN_SIZE)
                / Math.log(DOWNSAMPLE_BASE), Math.log(d.getWidth() / MIN_SIZE)
                / Math.log(DOWNSAMPLE_BASE));

        updateSize();
    }

    private void updateSize() {
        Dimension d = wsd.getBaselineDimension();
        double downsample = getDownsample();
        d.height /= downsample;
        d.width /= downsample;

        // System.out.println(d);

        setMinimumSize(d);
        setPreferredSize(d);
        setSize(d);
        // setMaximumSize(d);
        revalidate();
        repaint();
    }

    public static void main(String[] args) {
        File f = new File(args[0]);

        final Wholeslide w = new Wholeslide(f);

        JFrame j = new JFrame("OMG");

        final Test t = new Test(w);
        final JScrollPane jsp = new JScrollPane(t);
        jsp.setWheelScrollingEnabled(false);

        MouseAdapter m = new MouseAdapter() {
            private int x;

            private int y;

            private int sbx;

            private int sby;

            @Override
            public void mousePressed(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) {
                    return;
                }

                // System.out.println(e);
                x = e.getX();
                y = e.getY();

                JScrollBar h = jsp.getHorizontalScrollBar();
                sbx = h.getValue();
                JScrollBar v = jsp.getVerticalScrollBar();
                sby = v.getValue();

                h.setValueIsAdjusting(true);
                v.setValueIsAdjusting(true);
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) {
                    return;
                }

                int newX = sbx + x - e.getX();
                int newY = sby + y - e.getY();

                System.out.println(newX + " " + newY);

                JScrollBar h = jsp.getHorizontalScrollBar();
                JScrollBar v = jsp.getVerticalScrollBar();

                h.setValue(newX);
                v.setValue(newY);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) {
                    return;
                }

                JScrollBar h = jsp.getHorizontalScrollBar();
                JScrollBar v = jsp.getHorizontalScrollBar();

                h.setValueIsAdjusting(false);
                v.setValueIsAdjusting(false);
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                MouseEvent e2 = SwingUtilities.convertMouseEvent(e
                        .getComponent(), e, t);

                int amount = e.getWheelRotation();

                double oldDS = t.getDownsample();

                int bx = (int) (e2.getX() * oldDS);
                int by = (int) (e2.getY() * oldDS);

                t.adjustDownsample(amount);

                double newDS = t.getDownsample();

                System.out.println("oldDS: " + oldDS);
                System.out.println("newDS: " + newDS);
                System.out.println();

                JScrollBar hs = jsp.getHorizontalScrollBar();
                JScrollBar vs = jsp.getVerticalScrollBar();
                hs.setValue((int) (bx / newDS) - e.getX());
                vs.setValue((int) (by / newDS) - e.getY());
            }
        };

        jsp.getViewport().addMouseListener(m);
        jsp.getViewport().addMouseMotionListener(m);
        jsp.getViewport().addMouseWheelListener(m);

        j.getContentPane().add(jsp);
        j.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        j.setVisible(true);
        j.setSize(800, 600);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;

        int offsetX = 0;
        int offsetY = 0;

        Dimension d = wsd.getBaselineDimension();
        double downsample = getDownsample();
        d.width /= downsample;
        d.height /= downsample;

        int w = getWidth();
        int h = getHeight();

        if (w > d.width) {
            offsetX = (w - d.width) / 2;
        }
        if (h > d.height) {
            offsetY = (h - d.height) / 2;
        }

        Rectangle clip = g2.getClipBounds();

        g2.setColor(Color.BLACK);
        int rectVal = 3;
        g2.fillRect(offsetX + rectVal, offsetY + rectVal, d.width, d.height);

        // System.out.println(clip);
        wsd.paintRegion(g2, clip.x, clip.y, clip.x - offsetX, clip.y - offsetY,
                clip.width, clip.height, downsample);
    }
}
