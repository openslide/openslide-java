package edu.cmu.cs.wholeslide.gui;

import java.awt.BorderLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JFrame;
import javax.swing.JLabel;
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
            final WholeslideView wv = new WholeslideView(new Wholeslide(
                    new File(args[0])), true);
            wv.setBorder(BorderFactory.createTitledBorder(args[0]));
            jf.getContentPane().add(wv);

            final JLabel l = new JLabel(" ");
            jf.getContentPane().add(l, BorderLayout.SOUTH);
            wv.addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    long x = wv.getSlideX(e.getX());
                    long y = wv.getSlideY(e.getY());
                    l.setText("(" + x + "," + y + ")");
                }
            });
            wv.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseExited(MouseEvent e) {
                    l.setText(" ");
                }
            });

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
