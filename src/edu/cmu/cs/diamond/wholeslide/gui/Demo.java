package edu.cmu.cs.diamond.wholeslide.gui;

import java.io.File;

import javax.swing.JFrame;

import edu.cmu.cs.diamond.wholeslide.Wholeslide;

public class Demo {
    public static void main(String[] args) {
        JFrame jf = new JFrame("zzz");
        jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        jf.getContentPane().add(
                new WholeslideView(new Wholeslide(new File(args[0]))));
        
        jf.setSize(800, 600);
        
        jf.setVisible(true);
    }
}
