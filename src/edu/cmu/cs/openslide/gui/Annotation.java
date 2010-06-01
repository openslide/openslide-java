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
            Map<String, String> annotations;
            if (this.annotations != null) {
                annotations = new HashMap<String, String>();
                for (int i = 0; i < this.annotations.length; i++) {
                    Pair p = this.annotations[i];
                    annotations.put(p.getName(), p.getValue());
                }
            } else {
                annotations = Collections.emptyMap();
            }

            String[] segs = shape.split(" ");
            Path2D p = new Path2D.Double();

            int i = 0;
            while (i < segs.length) {
                switch (segs[i++].charAt(0)) {
                case 'Z':
                    p.closePath();
                    break;
                case 'C':
                    p.curveTo(Double.parseDouble(segs[i++]), Double
                            .parseDouble(segs[i++]), Double
                            .parseDouble(segs[i++]), Double
                            .parseDouble(segs[i++]), Double
                            .parseDouble(segs[i++]), Double
                            .parseDouble(segs[i++]));
                    break;
                case 'L':
                    p.lineTo(Double.parseDouble(segs[i++]), Double
                            .parseDouble(segs[i++]));
                    break;
                case 'M':
                    p.moveTo(Double.parseDouble(segs[i++]), Double
                            .parseDouble(segs[i++]));
                    break;
                case 'Q':
                    p.quadTo(Double.parseDouble(segs[i++]), Double
                            .parseDouble(segs[i++]), Double
                            .parseDouble(segs[i++]), Double
                            .parseDouble(segs[i++]));
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

            while (!p.isDone()) {
                double coords[] = new double[6];

                switch (p.currentSegment(coords)) {
                case PathIterator.SEG_CLOSE:
                    sb.append(" Z");
                    break;
                case PathIterator.SEG_CUBICTO:
                    sb.append(" C");
                    sb.append(" " + coords[0]);
                    sb.append(" " + coords[1]);
                    sb.append(" " + coords[2]);
                    sb.append(" " + coords[3]);
                    sb.append(" " + coords[4]);
                    sb.append(" " + coords[5]);
                    break;
                case PathIterator.SEG_LINETO:
                    sb.append(" L");
                    sb.append(" " + coords[0]);
                    sb.append(" " + coords[1]);
                    break;
                case PathIterator.SEG_MOVETO:
                    sb.append(" M");
                    sb.append(" " + coords[0]);
                    sb.append(" " + coords[1]);
                    break;
                case PathIterator.SEG_QUADTO:
                    sb.append(" Q");
                    sb.append(" " + coords[0]);
                    sb.append(" " + coords[1]);
                    sb.append(" " + coords[2]);
                    sb.append(" " + coords[3]);
                    sb.append(" " + coords[4]);
                    sb.append(" " + coords[5]);
                    break;
                }

                p.next();
            }
            return sb.toString().trim();
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
