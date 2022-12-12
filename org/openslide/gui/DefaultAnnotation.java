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

import java.awt.Shape;

// very minimal class, does not do defensive copying of shape
class DefaultAnnotation implements Annotation {
    final private Shape shape;

    public DefaultAnnotation(Shape shape) {
        if (shape == null) {
            throw new NullPointerException("shape cannot be null");
        }
        this.shape = shape;
    }

    @Override
    public Shape getShape() {
        return shape;
    }

    @Override
    public String toString() {
        return shape.toString();
    }
}
