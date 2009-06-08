package edu.cmu.cs.openslide;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.*;

class AssociatedImageMap implements Map<String, BufferedImage> {
    private static class ImageEntry implements Entry<String, BufferedImage> {

        final private String key;

        final private BufferedImage value;

        public ImageEntry(String key, BufferedImage value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public String getKey() {
            return key;
        }

        @Override
        public BufferedImage getValue() {
            return value;
        }

        @Override
        public BufferedImage setValue(BufferedImage value) {
            throw new UnsupportedOperationException();
        }
    }

    final private Set<String> names;

    final private OpenSlide os;

    public AssociatedImageMap(Set<String> names, OpenSlide os) {
        this.names = names;
        this.os = os;
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsKey(Object key) {
        return names.contains(key);
    }

    @Override
    public boolean containsValue(Object value) {
        if (value instanceof BufferedImage) {
            BufferedImage biVal = (BufferedImage) value;

            Collection<BufferedImage> vals = values();
            for (BufferedImage b : vals) {
                if (b.getWidth() != biVal.getWidth()) {
                    return false;
                }
                if (b.getHeight() != biVal.getHeight()) {
                    return false;
                }
                if (b.getType() != biVal.getType()) {
                    return false;
                }
                int data1[] = ((DataBufferInt) b.getRaster().getDataBuffer())
                        .getData();
                int data2[] = ((DataBufferInt) biVal.getRaster()
                        .getDataBuffer()).getData();

                return Arrays.equals(data1, data2);
            }
        }
        return false;
    }

    @Override
    public Set<Entry<String, BufferedImage>> entrySet() {
        Set<Entry<String, BufferedImage>> result = new HashSet<Entry<String, BufferedImage>>();

        for (String name : names) {
            Entry<String, BufferedImage> entry = new ImageEntry(name, get(name));
            result.add(entry);
        }

        return result;
    }

    @Override
    public BufferedImage get(Object key) {
        if (key instanceof String) {
            return os.getAssociatedImage((String) key);
        } else {
            return null;
        }
    }

    @Override
    public boolean isEmpty() {
        return names.isEmpty();
    }

    @Override
    public Set<String> keySet() {
        return names;
    }

    @Override
    public BufferedImage put(String key, BufferedImage value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void putAll(Map<? extends String, ? extends BufferedImage> m) {
        throw new UnsupportedOperationException();
    }

    @Override
    public BufferedImage remove(Object key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int size() {
        return names.size();
    }

    @Override
    public Collection<BufferedImage> values() {
        List<BufferedImage> values = new ArrayList<BufferedImage>();
        for (String name : names) {
            values.add(get(name));
        }

        return Collections.unmodifiableCollection(values);
    }
}
