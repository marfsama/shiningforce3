package com.sf3.gamedata.background;

import com.sf3.gamedata.mpd.DecompressedStream;
import com.sf3.util.ByteArrayImageInputStream;
import com.sf3.util.Sf3Util;

import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Read uncompressed background images: x4en*.bin */
public class BackgroundTexture extends JPanel {

    private final byte[] data;
    private final int[] palette;

    private int scale = 4;

    public BackgroundTexture(byte[] data, int[] palette) {
        this.data = data;
        this.palette = palette;
    }


    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setTransform(AffineTransform.getScaleInstance(scale, scale));
        int width = getWidth() / scale;
        BufferedImage image = createBufferedImage(width, true, true);

        g2d.drawImage(image,0,0,this);
    }

    private BufferedImage createBufferedImage(int width, boolean compressed, boolean palette) {
        try {
            ImageInputStream stream = new ByteArrayImageInputStream(data);
            stream.setByteOrder(ByteOrder.BIG_ENDIAN);
            int height = data.length / width;
            if (!palette) {
                height /= 2;
            }
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    if (palette) {
                        int value = stream.readUnsignedByte();
                        image.setRGB(x, y, this.palette[value]);
                    } else {
                        int value = stream.readUnsignedShort();
                        image.setRGB(x, y, Sf3Util.rgb16ToRgb24(value));
                    }

                }
            }
            return image;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static int[] readPalette(ImageInputStream stream) throws IOException {
        int[] palette = new int[256];
        for (int i = 0; i < palette.length; i++) {
            palette[i] = Sf3Util.rgb16ToRgb24(stream.readUnsignedShort());
        }
        return palette;
    }

    private static int[] readCompressedPalette(ImageInputStream stream) throws IOException {
        DecompressedStream decompressor = new DecompressedStream(stream);
        if (decompressor.getSize() != 0x200) {
            throw new IllegalStateException("decompressed palette size must be 512 ("+decompressor.getSize()+")");
        }
        ImageInputStream decompressedStream = decompressor.toStream();

        int[] palette = new int[256];
        for (int i = 0; i < palette.length; i++) {
            palette[i] = Sf3Util.rgb16ToRgb24(decompressedStream.readUnsignedShort());
        }
        return palette;
    }

    public static void main(String[] args) throws IOException {
        String basePath = System.getProperty("user.home")+"/project/games/shiningforce3/data/disk/bin/x4-background";
        String fileName = "x4blue.bin";
        Path path = Paths.get(basePath, fileName);

        try (InputStream inputStream = Files.newInputStream(path)) {
            ImageInputStream stream = new MemoryCacheImageInputStream(inputStream);
            stream.setByteOrder(ByteOrder.BIG_ENDIAN);

            stream.seek(0x248);
            int[] palette = readPalette(stream);

            stream.seek(0x3ba);
            byte[] data;
            boolean compressed = true;
            if (!compressed) {
                data = new byte[(int) (Files.size(path) - stream.getStreamPosition())];
                stream.readFully(data);
            } else {
                long start = stream.getStreamPosition();
                DecompressedStream decompressor = new DecompressedStream(stream);
                long end = stream.getStreamPosition();
                System.out.printf("compressed: %d decompressed %d %n", end - start, decompressor.getSize());
                data = decompressor.getResult();
                Files.write(Paths.get(basePath, fileName+".part1"), data);
            }

            JFrame frame = new JFrame("Background Texture");
            frame.setSize(100, 100);
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.add(new BackgroundTexture(data, palette));
            frame.setVisible(true);
        }
    }

}
