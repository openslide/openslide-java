/*
 *  OpenSlide, a library for reading whole slide image files
 *
 *  Copyright (c) 2007-2010 Carnegie Mellon University
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.AbstractListModel;

public class DefaultSelectionListModel extends AbstractListModel implements
        SelectionListModel {

    private final List<Annotation> list = new ArrayList<Annotation>();

    @Override
    public Annotation getElementAt(int index) {
        return list.get(index);
    }

    @Override
    public int getSize() {
        return list.size();
    }

    @Override
    public void add(Annotation s) {
        list.add(s);
        int i = list.size() - 1;
        fireIntervalAdded(this, i, i);
    }

    @Override
    public void add(int index, Annotation s) {
        list.add(index, s);
        fireIntervalAdded(this, index, index);
    }

    @Override
    public void remove(int index) {
        list.remove(index);
        fireIntervalRemoved(this, index, index);
    }

    @Override
    public void replace(int index, Annotation annotation) {
        list.remove(index);
        list.add(index, annotation);
        fireContentsChanged(this, index, index);
    }

    @Override
    public void clear() {
        int oldSize = list.size();
        list.clear();
        fireIntervalRemoved(this, 0, oldSize - 1);
    }

    @Override
    public boolean isEmpty() {
        return list.isEmpty();
    }

    @Override
    public Annotation get(int index) {
        return list.get(index);
    }

    @Override
    public Iterator<Annotation> iterator() {
        return list.iterator();
    }
}
