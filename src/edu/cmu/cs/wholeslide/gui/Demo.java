/*
 *  Wholeslide, a library for reading whole slide image files
 *
 *  Copyright (c) 2007-2008 Carnegie Mellon University
 *  All rights reserved.
 *
 *  Wholeslide is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, version 2.
 *
 *  Wholeslide is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Wholeslide. If not, see <http://www.gnu.org/licenses/>.
 *
 *  Linking Wholeslide statically or dynamically with other modules is
 *  making a combined work based on Wholeslide. Thus, the terms and
 *  conditions of the GNU General Public License cover the whole
 *  combination.
 */

package edu.cmu.cs.wholeslide.gui;

import java.awt.BorderLayout;
import java.awt.event.*;
import java.io.File;

import javax.swing.*;

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
