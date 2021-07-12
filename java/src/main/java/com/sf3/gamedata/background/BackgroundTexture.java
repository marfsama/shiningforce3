package com.sf3.gamedata.background;

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
import java.util.Arrays;
import java.util.List;

/** Read uncompressed background images: x4en*.bin */
public class BackgroundTexture extends JPanel {

    private final BufferedImage image;

    public BackgroundTexture(BufferedImage image) {
        this.image = image;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setTransform(AffineTransform.getScaleInstance(3.0, 3.0));
        g2d.drawImage(image,0,0,this);
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(256*3,256*3);
    }

    @Override
    public Dimension getPreferredSize() {
        return getMinimumSize();
    }

    public static void main(String[] args) throws IOException {
        String basePath = System.getProperty("user.home")+"/project/games/shiningforce3/data/disk/bin";
        List<String> fileNames = Arrays.asList(
                "x4en001.bin", "x4en015.bin", "x4en107.bin");
        Path path = Paths.get(basePath, fileNames.get(1));

        try (InputStream inputStream = Files.newInputStream(path)) {
            ImageInputStream stream = new MemoryCacheImageInputStream(inputStream);
            stream.setByteOrder(ByteOrder.BIG_ENDIAN);

            int[] palette = readPallete(stream);
            BufferedImage image = new BufferedImage(512, 256, BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < 256; y++) {
                for (int x = 0; x < 512; x++) {
                    int value = stream.readUnsignedByte();
                    image.setRGB(x,y,palette[value]);
                }
            }

            JFrame frame = new JFrame("Background Texture");
            frame.setSize(1000, 1000);
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.add(new BackgroundTexture(image));
            frame.setVisible(true);
        }
    }

    private static int[] readPallete(ImageInputStream stream) throws IOException {
        int[] palette = new int[256];
        for (int i = 0; i < palette.length; i++) {
            palette[i] = Sf3Util.rgb16ToRgb24(stream.readUnsignedShort());
        }
        return palette;
    }

}
