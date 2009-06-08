/*
 *  OpenSlide, a library for reading whole slide image files
 *
 *  Copyright (c) 2007-2008 Carnegie Mellon University
 *  All rights reserved.
 *
 *  OpenSlide is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, version 2.
 *
 *  OpenSlide is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with OpenSlide. If not, see <http://www.gnu.org/licenses/>.
 *
 *  Linking OpenSlide statically or dynamically with other modules is
 *  making a combined work based on OpenSlide. Thus, the terms and
 *  conditions of the GNU General Public License cover the whole
 *  combination.
 */

package edu.cmu.cs.openslide.gui;

import java.awt.BorderLayout;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.*;

import edu.cmu.cs.openslide.OpenSlide;

public class Demo {
    public static void main(String[] args) {
        JFrame jf = new JFrame("OpenSlide");
        jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        OpenSlide os;
        switch (args.length) {
        case 0:
            System.out.println("Give 1 or 2 files");
            return;

        case 1:
            os = new OpenSlide(new File(args[0]));
            final OpenSlideView wv = new OpenSlideView(os, true);
            wv.setBorder(BorderFactory.createTitledBorder(args[0]));
            jf.getContentPane().add(wv);

            final JLabel l = new JLabel(" ");
            System.out.println("comment: " + os.getComment());
            System.out.println("properties:");
            System.out.println(os.getProperties());

            jf.getContentPane().add(new JLabel(os.getComment()),
                    BorderLayout.NORTH);
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

            Map<String, BufferedImage> associatedImages = os
                    .getAssociatedImages();
            for (Entry<String, BufferedImage> e : associatedImages.entrySet()) {
                JFrame j = new JFrame(e.getKey());
                j.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                j.add(new JLabel(new ImageIcon(e.getValue())));
                j.pack();
                j.setVisible(true);
            }

            break;

        case 2:
            final OpenSlideView w1 = new OpenSlideView(new OpenSlide(new File(
                    args[0])), true);
            final OpenSlideView w2 = new OpenSlideView(new OpenSlide(new File(
                    args[1])), true);
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
