package edu.cmu.cs.diamond.wholeslide;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.io.File;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

public class Test extends JPanel {

    final private Wholeslide wsd;

    private double downsample = 1.0;

    public Test(Wholeslide w) {
        wsd = w;
        
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
        setMaximumSize(d);
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

            @Override
            public void mousePressed(MouseEvent e) {
//                System.out.println(e);
                x = e.getXOnScreen();
                y = e.getYOnScreen();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
//                System.out.println(e);

                int newX = e.getXOnScreen();
                int newY = e.getYOnScreen();
                int relX = newX - x;
                int relY = newY - y;
                x = newX;
                y = newY;

                Point p = jsp.getViewport().getViewPosition();
                System.out.println(p);
                p.x -= relX;
                p.y -= relY;
                jsp.getViewport().setViewPosition(p);
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

        t.addMouseListener(m);
        t.addMouseMotionListener(m);
        t.addMouseWheelListener(m);

        j.getContentPane().add(jsp);
        j.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        j.setVisible(true);
        j.setSize(800, 600);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;

        Rectangle clip = g2.getClipBounds();
        System.out.println(clip);
        wsd.paintRegion(g2, clip.x, clip.y, clip.x, clip.y, clip.width,
                clip.height, downsample);
    }
}
