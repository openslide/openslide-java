package edu.cmu.cs.diamond.wholeslide;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
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
        j.getContentPane().add(new JScrollPane(new Test(w)));
        j.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        j.setVisible(true);
        j.setSize(800, 600);
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        Graphics2D g2 = (Graphics2D) g;
        
        Rectangle clip = g2.getClipBounds();
        wsd.paintRegion(g2, clip.x, clip.y, clip.x, clip.y, clip.width, clip.height, 1.0);
    }
}
