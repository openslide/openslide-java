package edu.cmu.cs.openslide.gui;

import java.awt.Shape;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.AbstractListModel;

public class SelectionListModel extends AbstractListModel implements
        Iterable<Shape> {

    private final List<Shape> list = new ArrayList<Shape>();

    @Override
    public Shape getElementAt(int index) {
        return list.get(index);
    }

    @Override
    public int getSize() {
        return list.size();
    }

    public void add(Shape s) {
        list.add(s);
        int i = list.size() - 1;
        fireIntervalAdded(this, i, i);
    }

    public void clear() {
        int oldSize = list.size();
        list.clear();
        fireIntervalRemoved(this, 0, oldSize - 1);
    }

    public boolean isEmpty() {
        return list.isEmpty();
    }

    public Shape get(int index) {
        return list.get(index);
    }

    @Override
    public Iterator<Shape> iterator() {
        return list.iterator();
    }
}
