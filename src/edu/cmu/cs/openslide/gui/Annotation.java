package edu.cmu.cs.openslide.gui;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

public class Annotation {
    final private String text;

    final private Shape shape;

    public Annotation(Shape shape) {
        this(shape, null);
    }

    public Annotation(Shape shape, String text) {
        if (shape == null) {
            throw new NullPointerException("shape cannot be null");
        }

        this.text = text;
        this.shape = new ImmutableShape(shape);
    }

    public String getText() {
        return text;
    }

    public Shape getShape() {
        return shape;
    }

    final private static class ImmutableShape implements Shape {
        final private Shape shape;

        public boolean contains(double x, double y, double w, double h) {
            return shape.contains(x, y, w, h);
        }

        public boolean contains(double x, double y) {
            return shape.contains(x, y);
        }

        public boolean contains(Point2D p) {
            return shape.contains(p);
        }

        public boolean contains(Rectangle2D r) {
            return shape.contains(r);
        }

        public Rectangle getBounds() {
            return shape.getBounds();
        }

        public Rectangle2D getBounds2D() {
            return shape.getBounds2D();
        }

        public PathIterator getPathIterator(AffineTransform at, double flatness) {
            return shape.getPathIterator(at, flatness);
        }

        public PathIterator getPathIterator(AffineTransform at) {
            return shape.getPathIterator(at);
        }

        public boolean intersects(double x, double y, double w, double h) {
            return shape.intersects(x, y, w, h);
        }

        public boolean intersects(Rectangle2D r) {
            return shape.intersects(r);
        }

        public ImmutableShape(Shape s) {
            shape = s;
        }
    }
}
