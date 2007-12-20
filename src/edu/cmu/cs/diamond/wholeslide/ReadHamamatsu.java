package edu.cmu.cs.diamond.wholeslide;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.LongBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReadHamamatsu {

    static private class VMS {

        private boolean noLayers;

        private boolean noJpegColumns;

        private boolean noJpegRows;

        private String imageFile;

        private String mapFile;

        private String optimisationFile;

        private String authCode;

        private double sourceLens;

        private long physicalWidth;

        private long physicalHeight;

        private long layerSpacing;

        private String macroImage;

        private long physicalMacroWidth;

        private long physicalMacroHeight;

        private long xOffsetFromSlideCentre;

        private long yOffsetFromSlideCentre;

        private long optimizationOffsets[];

        // final static private Set<String> keyNames = Collections
        // .unmodifiableSet(new HashSet<String>(Arrays.asList("NoLayers",
        // "NoJpegColumns", "NoJpegRows", "ImageFile", "MapFile",
        // "OptimisationFile", "AuthCode", "SourceLens",
        // "PhysicalWidth", "PhysicalHeight", "LayerSpacing",
        // "MacroImage", "PhysicalMacroWidth",
        // "PhysicalMacroHeight", "XOffsetFromSlideCentre",
        // "YOffsetFromSlideCentre")));

        public VMS(File f) throws IOException {
            parseIni(f);

            parseOptimisationFile();
        }

        private void parseOptimisationFile() throws IOException {
            FileInputStream fin = new FileInputStream(optimisationFile);
            FileChannel fc = fin.getChannel();
            ByteBuffer bb = ByteBuffer.allocateDirect(8);
            bb.order(ByteOrder.LITTLE_ENDIAN);

            List<Long> list = new ArrayList<Long>();

            TOP: while (true) {
                while (true) {
                    int amount = fc.read(bb);
                    if (amount == -1) {
                        break TOP;
                    }
                    if (bb.position() == 8) {
                        break;
                    }
                }
                long l = bb.getLong(0);
                bb.rewind();
                if (l != 0) {
                    list.add(l);
                }
            }

            optimizationOffsets = new long[list.size()];
            for (int i = 0; i < optimizationOffsets.length; i++) {
                optimizationOffsets[i] = list.get(i);
            }
        }

        private void parseIni(File f) throws FileNotFoundException, IOException {
            Pattern p = Pattern.compile("([^=]+)=(.*)");

            // parse ini
            BufferedReader reader = new BufferedReader(new FileReader(f));

            String line;

            boolean active = false;

            try {
                while ((line = reader.readLine()) != null) {
                    line = line.trim();

                    // find header
                    if (line.startsWith("[")) {
                        active = line.equals("[Virtual Microscope Specimen]");
                        continue;
                    }

                    if (!active) {
                        continue;
                    }

                    // read key/value
                    Matcher m = p.matcher(line);
                    if (!m.matches()) {
                        continue;
                    }
                    String key = m.group(1).intern();
                    String value = m.group(2);

                    if (key.equals("OptimisationFile")) {
                        optimisationFile = value;
                    } else if (key.equals("ImageFile")) {
                        imageFile = value;
                    }
                }
            } finally {
                reader.close();
            }
        }

        public void splitImageFile() throws IOException {
            DecimalFormat format = new DecimalFormat();
            format.setMinimumIntegerDigits((int) Math.ceil(Math
                    .log10(optimizationOffsets.length)));
            format.setGroupingUsed(false);

            InputStream img = new BufferedInputStream(new FileInputStream(
                    imageFile));

            // datas
            long pos = 0;
            byte header[] = new byte[(int) optimizationOffsets[0]];

            for (int i = 0; i <= optimizationOffsets.length; i++) {
                String suffix;
                boolean firstOne = false;
                if (i == 0) {
                    suffix = "tables";
                    firstOne = true;
                } else {
                    suffix = format.format(i - 1);
                }

                File outFile = new File(imageFile + "-" + suffix + ".jpg");
                OutputStream out = new BufferedOutputStream(
                        new FileOutputStream(outFile));
                System.out.println(outFile);

                long limit;
                boolean lastOne = false;
                if (i == optimizationOffsets.length) {
                    limit = -1;
                    lastOne = true;
                } else {
                    limit = optimizationOffsets[i];
                }

                // SOI
                // if (!firstOne) {
                // out.write(0xFF);
                // out.write(0xD8);
                // }

                // write out header
                if (!firstOne) {
                    out.write(header);
                }

                byte restartValue = 0;
                boolean lastWasFF = false;
                while (pos < limit || lastOne) {
                    int b = img.read();
                    if (b == -1) {
                        break;
                    }

                    if (firstOne) {
                        // save header
                        header[(int) pos] = (byte) b;
                    }

                    // rewrite restart markers
                    if (lastWasFF && b >= 0xD0 && b < 0xD8) {
                        int newVal = 0xD0 | restartValue;
                        // System.out.println("restart: " + b + " -> " +
                        // newVal);
                        b = newVal;
                        restartValue = (byte) ((restartValue + 1) % 8);
                    }

                    out.write(b);
                    lastWasFF = b == 0xFF;
                    pos++;
                }

                if (firstOne) {
                    rewriteHeader(header);
                }

                // EOI
                if (!lastOne) {
                    out.write(0xFF);
                    out.write(0xD9);
                }

                out.close();
            }

            img.close();
        }

        private void rewriteHeader(byte[] header) {
            // find SOF
            for (int i = 0; i < header.length; i++) {
                if ((header[i] & 0xFF) != 0xFF) {
                    continue;
                } else if ((header[i + 1] & 0xFF) != 0xC0) {
                    continue;
                }

                // rewrite height to 8
                header[i + 5] = 0;
                header[i + 6] = 8;
                System.out.println("ok");
                break;
            }
        }

        public void printOptimizationOffsets() {
            System.out.println(Arrays.toString(optimizationOffsets));
        }

        public void printRestartMarkers() throws IOException {
            InputStream img = new BufferedInputStream(new FileInputStream(
                    imageFile));

            int b;
            long pos = 0;
            int count = 0;
            boolean lastWasFF = false;
            while ((b = img.read()) != -1) {
                if (lastWasFF && b >= 0xD0 && b < 0xD8) {
                    System.out.println(pos);
                    count++;
                }
                lastWasFF = b == 0xFF;
                pos++;
            }

            System.out.println("total: " + count);
            img.close();
        }
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        try {
            File f = new File(args[0]);

            VMS vms = new VMS(f);
            // vms.splitImageFile();
            vms.printRestartMarkers();
            vms.printOptimizationOffsets();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
