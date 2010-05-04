package edu.cmu.cs.openslide.gui;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Annotation {
    public static class Bean {
        public static class Pair {
            public Pair(String name, String value) {
                this.name = name;
                this.value = value;
            }

            public Pair() {
            };

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }

            public String getValue() {
                return value;
            }

            public void setValue(String value) {
                this.value = value;
            }

            private String name;

            private String value;
        }

        private Pair annotations[];

        private String shape;

        public String getShape() {
            return shape;
        }

        public Pair[] getAnnotations() {
            return annotations;
        }

        public void setAnnotations(Pair[] annotations) {
            this.annotations = annotations;
        }

        public void setShape(String shape) {
            this.shape = shape;
        }

        public Bean() {
        }

        public Annotation toAnnotation() {
            Map<String, String> annotations = null;
            if (this.annotations != null) {
                annotations = new HashMap<String, String>();
                for (int i = 0; i < this.annotations.length; i++) {
                    Pair p = this.annotations[i];
                    annotations.put(p.getName(), p.getValue());
                }
            }

            String[] segs = shape.split(" ");
            Path2D p = new Path2D.Double(Integer.parseInt(segs[0]));
            for (int i = 1; i < segs.length; i += 7) {
                double x1 = Double.parseDouble(segs[i + 1]);
                double y1 = Double.parseDouble(segs[i + 2]);
                double x2 = Double.parseDouble(segs[i + 3]);
                double y2 = Double.parseDouble(segs[i + 4]);
                double x3 = Double.parseDouble(segs[i + 5]);
                double y3 = Double.parseDouble(segs[i + 6]);
                switch (Integer.parseInt(segs[i])) {
                case PathIterator.SEG_CLOSE:
                    p.closePath();
                    break;
                case PathIterator.SEG_CUBICTO:
                    p.curveTo(x1, y1, x2, y2, x3, y3);
                    break;
                case PathIterator.SEG_LINETO:
                    p.lineTo(x1, y1);
                    break;
                case PathIterator.SEG_MOVETO:
                    p.moveTo(x1, y1);
                    break;
                case PathIterator.SEG_QUADTO:
                    p.quadTo(x1, y1, x2, y2);
                    break;
                }
            }

            return new Annotation(p, annotations);
        }

        Bean(Shape shape, Map<String, String> annotations) {
            this.shape = serialize(shape);
            this.annotations = new Pair[annotations.size()];
            int i = 0;
            for (Map.Entry<String, String> e : annotations.entrySet()) {
                this.annotations[i] = new Pair(e.getKey(), e.getValue());
                i++;
            }
        }

        private static String serialize(Shape s) {
            PathIterator p = s.getPathIterator(null);
            StringBuilder sb = new StringBuilder();
            sb.append(p.getWindingRule());

            while (!p.isDone()) {
                double coords[] = new double[6];
                sb.append(" ");
                sb.append(p.currentSegment(coords));
                for (int i = 0; i < 6; i++) {
                    sb.append(" ");
                    sb.append(coords[i]);
                }
                p.next();
            }
            return sb.toString();
        }
    }

    final private Map<String, String> annotations;

    final private Shape shape;

    final static private Map<String, String> EMPTY_MAP = Collections.emptyMap();

    public Annotation(Shape shape, Map<String, String> annotations) {
        if (shape == null) {
            throw new NullPointerException("shape cannot be null");
        }
        this.shape = new ImmutableShape(shape);

        this.annotations = Collections.unmodifiableMap(annotations);
    }

    public Annotation(Shape shape) {
        this(shape, EMPTY_MAP);
    }

    public Map<String, String> getAnnotations() {
        return annotations;
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

    public Bean toBean() {
        return new Bean(shape, annotations);
    }
}
