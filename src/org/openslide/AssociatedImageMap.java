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

package org.openslide;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;

public class AssociatedImageMap {

    final private Set<String> names;

    final private OpenSlide os;

    AssociatedImageMap(Set<String> names, OpenSlide os) {
        this.names = names;
        this.os = os;
    }

    public boolean isEmpty() {
        return names.isEmpty();
    }

    public int size() {
        return names.size();
    }

    public boolean contains(String name) {
        return names.contains(name);
    }

    public BufferedImage get(String name) throws IOException {
        if (contains(name)) {
            return os.getAssociatedImage(name);
        } else {
            return null;
        }
    }

    public Set<String> getNames() {
        return names;
    }

    @Override
    public int hashCode() {
        return os.hashCode() + 12;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj instanceof AssociatedImageMap) {
            AssociatedImageMap map2 = (AssociatedImageMap) obj;
            return os.equals(map2.os);
        }

        return false;
    }
}
