package edu.cmu.cs.diamond.wholeslide;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.io.File;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;

public class Test extends JPanel {

    final private Wholeslide wsd;
    
    private double downsample = 1.0;

    public Test(Wholeslide w) {
        wsd = w;
        
        setBackground(wsd.getBackgroundColor());

        updateSize();
    }

    private void updateSize() {
        Dimension d = wsd.getBaselineDimension();
        d.height /= downsample;
        d.width /= downsample;

        System.out.println(downsample);
        System.out.println(d);
        
        setMinimumSize(d);
        setPreferredSize(d);
//        setMaximumSize(d);
        revalidate();
        repaint();
    }

    public static void main(String[] args) {
        File f = new File(args[0]);

        Wholeslide w = new Wholeslide(f);

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
//                System.out.println(e);
                x = e.getX();
                y = e.getY();
                sbx = jsp.getHorizontalScrollBar().getValue();
                sby = jsp.getVerticalScrollBar().getValue();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
//                System.out.println(e);

                int newX = e.getX();
                int newY = e.getY();
                int relX = sbx + x - newX;
                int relY = sby + y - newY;

                JScrollBar h = jsp.getHorizontalScrollBar();
                JScrollBar v = jsp.getVerticalScrollBar();

                h.setValue(relX);
                v.setValue(relY);
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                int amount = -e.getWheelRotation();

                if (amount > 0) {
                    t.downsample /= (amount * 1.2);
                } else {
                    t.downsample *= (-amount * 1.2);
                }
                
                if (t.downsample < 1) {
                    t.downsample = 1;
                }
                
                t.updateSize();
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
        System.out.println(clip);
        wsd.paintRegion(g2, clip.x, clip.y, clip.x - offsetX, clip.y - offsetY, clip.width,
                clip.height, downsample);
    }
}
