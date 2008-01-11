package edu.cmu.cs.diamond.wholeslide.gui;

import java.awt.BorderLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;

import javax.swing.Box;
import javax.swing.JFrame;
import javax.swing.JToggleButton;

import edu.cmu.cs.diamond.wholeslide.Wholeslide;

public class Demo {
    public static void main(String[] args) {
        JFrame jf = new JFrame("zzz");
        jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        switch (args.length) {
        case 0:
            System.out.println("oops");
            return;

        case 1:
            jf.getContentPane().add(
                    new WholeslideView(new Wholeslide(new File(args[0]))));
            break;

        case 2:
            final WholeslideView w1 = new WholeslideView(new Wholeslide(
                    new File(args[0])));
            final WholeslideView w2 = new WholeslideView(new Wholeslide(
                    new File(args[1])));
            Box b = Box.createHorizontalBox();
            b.add(w1);
            b.add(w2);
            jf.getContentPane().add(b);
            
            JToggleButton linker = new JToggleButton("Link");
            jf.getContentPane().add(linker, BorderLayout.SOUTH);
            linker.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    switch(e.getStateChange()) {
                    case ItemEvent.SELECTED:
                        w1.linkWithOther(w2);
                        break;
                    case ItemEvent.DESELECTED:
                        w1.unlinkOther();
                        break;
                    }
                }
            });
            
            break;

        default:
            return;
        }

        jf.setSize(800, 600);

        jf.setVisible(true);
    }
}
