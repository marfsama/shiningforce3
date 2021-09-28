package com.sf3.gamedata.sprites;

import com.sf3.binaryview.HighlightGroups;
import com.sf3.gamedata.utils.Block;
import com.sf3.gamedata.utils.HexValue;
import com.sf3.util.ByteArrayImageInputStream;
import com.sf3.util.Sf3Util;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SpriteUtils {

    /** write sprite sheet in groups of 4 images. */
    public static void writeSpriteSheet(Path out, String filename, int width, int height, List<BufferedImage> sprites) throws IOException {
        int index = 0;
        BufferedImage spriteSheet = new BufferedImage(width * 4, height * ((sprites.size()+3) / 4), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = spriteSheet.createGraphics();
        for (BufferedImage sprite : sprites) {
            int x = (index % 4) * width;
            int y = (index / 4) * height;
            graphics.drawImage(sprite, x, y, null);
            index++;
        }
        graphics.dispose();
        ImageIO.write(spriteSheet, "PNG", out.resolve("spritesheet_"+ filename +".png").toFile());
    }

    public static BufferedImage readSprite(ImageInputStream stream, Block file, HighlightGroups highlights, int offset, int width, int height) throws IOException {
        stream.seek(offset);
        int colorDataSize = stream.readInt();
        file.addProperty(""+new HexValue(offset), new HexValue(colorDataSize)+" - "+new HexValue(offset+colorDataSize));

        highlights.addPointer("commands", offset, offset+colorDataSize);
        highlights.addRange("color_data", offset, colorDataSize);

        // read color data stream
        byte[] data = new byte[colorDataSize-4];
        stream.readFully(data);

        List<Integer> colors = decompressImage(data, stream);
        highlights.addRange("command_data", offset + colorDataSize, (int) stream.getStreamPosition() - offset - colorDataSize);

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < width; y++) {
            for (int x = 0; x < width; x++) {
                int index = y * width + x;
                if (index < colors.size()) {
                    image.setRGB(x, y, Sf3Util.rgb16ToRgb24(colors.get(index)));
                }
            }
        }
        return image;
    }

    public static List<Integer> decompressImage(byte[] colorData, ImageInputStream commands) throws IOException {
        List<Integer> colors = new ArrayList<>();

        // prepare command bitstream
        BitStream commandBitStream = new BitStream(commands);
        ColorStream colorStream = new ColorStream(colorData);

        while (colorStream.hasMore()) {
            int color = colorStream.nextColor();

            // copy single?
            if (commandBitStream.nextBit() == 0)
            {
                // copy single and continue
                colors.add(color);
                continue;
            }

            int bitLength = 1;

            // count number of '1', consuming the first '0' in the process.
            // Note: the bitLength is at least 1, when the copy bit is followed directly by a zero '0'
            while (commandBitStream.nextBit() != 0) {
                bitLength++;
            }

            // msb is always 1. As the bitLength os at least 1 too, the minimal count is
            // 10 binary or 2 decimal when the only count bit is '0'.
            int count = 1;

            // read number of bits for repeat count.
            for (int i = 0; i < bitLength; i++)
            {
                count <<= 1;
                if (commandBitStream.nextBit() == 1) {
                    // add the read bit as lsb
                    count += 1;
                }
            }

            // Output color
            colors.addAll(Collections.nCopies(count, color));
        }
        return colors;
    }

    private static class ColorStream {
        private final ImageInputStream reader;
        private final int streamSize;

        private int streamPosition;
        private int windowPosition;
        private List<Integer> colorWindow;
        private boolean first = true;


        public ColorStream(byte[] colorData) {
            this.reader = new ByteArrayImageInputStream(colorData);
            this.reader.setByteOrder(ByteOrder.BIG_ENDIAN); // for good measure, byte order doesn't matter here
            this.streamSize = colorData.length;
            // rotating color window has a size of 0x80, as only 7 bits are used as index
            colorWindow = new ArrayList<>(Collections.nCopies(0x80, 0));
            // fill default values.
            colorWindow.set(0x7f, 0x7fff);
            colorWindow.set(0x7e, 0x8000);
            colorWindow.set(0x7d, 0); // kinda redundant, as the default values for the list is zero.
            // start right before the default values
            windowPosition = 0;
            streamPosition = 0;
        }

        public boolean hasMore() {
            return streamPosition < streamSize;
        }

        public int nextColor() throws IOException {
            int colorByte = reader.readByte();
            streamPosition++;
            // index into rotating color window?
            if (colorByte >= 0)
            {
                // yes, return the color
                return colorWindow.get(colorByte);
            }
            // no. read second color byte and
            int color = (((colorByte & 0xff) << 8) | reader.readUnsignedByte()) & 0xffff;
            streamPosition++;
            // add color to color window
            colorWindow.set(windowPosition, color);

            // increase window position, rotating bacek to start on overflow. Default values are not part of the
            // rotating window.
            windowPosition = (windowPosition + 1) % 0x7d;
            return color;
        }
    }

    private static class BitStream {
        private final ImageInputStream reader;
        private int currentByte;
        private int remainingBits;

        public BitStream(ImageInputStream reader) {
            this.reader = reader;
            this.remainingBits = 0;
        }

        public int nextBit() throws IOException {
            if (remainingBits == 0) {
                currentByte = reader.readUnsignedByte();
                remainingBits = 8;
            }
            // extract msb
            int bit = (currentByte & 0x80) >> 7;
            // move msb out
            currentByte <<= 1;
            // reduce number of remaining bits in the current byte
            remainingBits--;
            return bit;
        }
    }

}
