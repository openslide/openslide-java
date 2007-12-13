package edu.cmu.cs.diamond.wholeslide;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

public class Test extends JPanel {

    final private Wholeslide wsd;

    public Test(Wholeslide w) {
        wsd = w;
        setMinimumSize(wsd.getBaselineDimension());
        setPreferredSize(wsd.getBaselineDimension());
    }

    public static void main(String[] args) {
        File f = new File(args[0]);

        Wholeslide w = new Wholeslide(f);

        JFrame j = new JFrame("OMG");

        Test t = new Test(w);
        final JScrollPane jsp = new JScrollPane(t);

        MouseAdapter m = new MouseAdapter() {
            private int x;
            private int y;

            @Override
            public void mousePressed(MouseEvent e) {
                System.out.println(e);
                x = e.getXOnScreen();
                y = e.getYOnScreen();
            }
        
            @Override
            public void mouseDragged(MouseEvent e) {
                System.out.println(e);
                
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
        };
        
        t.addMouseListener(m);
        t.addMouseMotionListener(m);

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
        wsd.paintRegion(g2, clip.x, clip.y, clip.x, clip.y, clip.width,
                clip.height, 1.0);
    }
}
