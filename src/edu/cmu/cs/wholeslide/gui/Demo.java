package edu.cmu.cs.wholeslide.gui;

import java.awt.BorderLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JFrame;
import javax.swing.JToggleButton;

import edu.cmu.cs.wholeslide.Wholeslide;

public class Demo {
    public static void main(String[] args) {
        JFrame jf = new JFrame("Wholeslide");
        jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        switch (args.length) {
        case 0:
            System.out.println("Give 1 or 2 files");
            return;

        case 1:
            WholeslideView wv = new WholeslideView(new Wholeslide(new File(
                    args[0])), true);
            wv.setBorder(BorderFactory.createTitledBorder(args[0]));
            jf.getContentPane().add(wv);

            break;

        case 2:
            final WholeslideView w1 = new WholeslideView(new Wholeslide(
                    new File(args[0])), true);
            final WholeslideView w2 = new WholeslideView(new Wholeslide(
                    new File(args[1])), true);
            Box b = Box.createHorizontalBox();
            b.add(w1);
            b.add(w2);
            jf.getContentPane().add(b);

            JToggleButton linker = new JToggleButton("Link");
            jf.getContentPane().add(linker, BorderLayout.SOUTH);
            linker.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    switch (e.getStateChange()) {
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

        jf.setSize(900, 700);

        jf.setVisible(true);
    }
}
