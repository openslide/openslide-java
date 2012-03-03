/*
 *  OpenSlide, a library for reading whole slide image files
 *
 *  Copyright (c) 2007-2011 Carnegie Mellon University
 *  All rights reserved.
 *
 *  OpenSlide is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation, version 2.1.
 *
 *  OpenSlide is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with OpenSlide. If not, see
 *  <http://www.gnu.org/licenses/>.
 *
 */

package org.openslide.gui;

import java.awt.BorderLayout;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

import javax.swing.*;

import org.openslide.OpenSlide;
import org.openslide.AssociatedImage;

public class Demo {
    public static void main(final String[] args) {
        // set application name on Mac OS X
        System.setProperty("com.apple.mrj.application.apple.menu.about.name",
                "OpenSlide Demo");
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                JFrame jf = new JFrame("OpenSlide");
                jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

                try {
                    switch (args.length) {
                    case 0:
                        JFileChooser jfc = new JFileChooser();
                        jfc.setAcceptAllFileFilterUsed(false);
                        jfc.setFileFilter(OpenSlide.getFileFilter());
                        int result = jfc.showDialog(null, "Open");
                        if (result == JFileChooser.APPROVE_OPTION) {
                            openOne(jfc.getSelectedFile(), jf);
                        } else {
                            return;
                        }
                        break;

                    case 1:
                        openOne(new File(args[0]), jf);
                        break;

                    case 2:
                        final OpenSlideView w1 = new OpenSlideView(
                                new OpenSlide(new File(args[0])), true);
                        final OpenSlideView w2 = new OpenSlideView(
                                new OpenSlide(new File(args[1])), true);
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
                } catch (IOException e1) {
                    e1.printStackTrace();
                    System.exit(1);
                }

                jf.setSize(900, 700);

                jf.setVisible(true);
            }

            private void openOne(File file, JFrame jf) throws IOException {
                OpenSlide os;
                os = new OpenSlide(file);
                final OpenSlideView wv = new OpenSlideView(os, true);
                wv.setBorder(BorderFactory.createTitledBorder(file.getName()));
                jf.getContentPane().add(wv);

                final JLabel l = new JLabel(" ");
                // System.out.println("properties:");
                // System.out.println(os.getProperties());

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

                for (AssociatedImage img : os.getAssociatedImages()
                        .values()) {
                    JFrame j = new JFrame(img.getName());
                    j.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                    j.add(new JLabel(new ImageIcon(img.toBufferedImage())));
                    j.pack();
                    j.setVisible(true);
                }

                Map<String, String> properties = os.getProperties();
                List<Object[]> propList = new ArrayList<Object[]>();
                SortedSet<Entry<String, String>> sorted = new TreeSet<Entry<String, String>>(
                        new Comparator<Entry<String, String>>() {
                            @Override
                            public int compare(Entry<String, String> o1,
                                    Entry<String, String> o2) {
                                String k1 = o1.getKey();
                                String k2 = o2.getKey();
                                return k1.compareTo(k2);
                            }
                        });
                sorted.addAll(properties.entrySet());
                for (Entry<String, String> e : sorted) {
                    propList.add(new Object[] { e.getKey(), e.getValue() });
                }
                JTable propTable = new JTable(propList
                        .toArray(new Object[1][0]), new String[] { "key",
                        "value" }) {
                    @Override
                    public boolean isCellEditable(int row, int column) {
                        return false;
                    }
                };
                JFrame propFrame = new JFrame("properties");
                propFrame.add(new JScrollPane(propTable));
                propFrame.pack();
                propFrame.setVisible(true);

                /*
                 * JFrame listFrame = new JFrame("selections");
                 * listFrame.add(new JScrollPane(new JList(wv
                 * .getSelectionListModel()))); listFrame.pack();
                 * listFrame.setVisible(true);
                 */
            }
        });
    }
}
