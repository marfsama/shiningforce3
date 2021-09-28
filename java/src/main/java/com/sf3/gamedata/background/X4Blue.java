package com.sf3.gamedata.background;

import com.sf3.binaryview.HighlightGroups;
import com.sf3.gamedata.mpd.DecompressedStream;
import com.sf3.gamedata.utils.Block;
import com.sf3.gamedata.utils.HexValue;
import com.sf3.util.Sf3Util;
import com.sf3.util.Utils;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class X4Blue {
    public static void main(String[] args) {
        String basePath = System.getProperty("user.home")+"/project/games/shiningforce3/data/disk/bin/x4-background";
        String outPath = System.getProperty("user.home") + "/project/games/shiningforce3/data";

        List<String> filenames = Arrays.asList("x4blue.bin", "x4sf3wk.bin");
        for (String fileName : filenames) {
            Path path = Paths.get(basePath, fileName);

            try (InputStream inputStream = Files.newInputStream(path)) {
                ImageInputStream stream = new MemoryCacheImageInputStream(inputStream);
                stream.setByteOrder(ByteOrder.BIG_ENDIAN);
                Path out = Paths.get(outPath, fileName);
                Files.createDirectories(out);

                Block file = new X4Blue().readFile(path, out, fileName);
                Files.write(out.resolve(fileName + ".json"), Utils.toPrettyFormat(file.toString()).getBytes(StandardCharsets.UTF_8));

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
    private Block readFile(Path path, Path out, String filename) {
        try (InputStream is = Files.newInputStream(path)) {
            Block file = new Block(path.getFileName().toString(), 0, (int) Files.size(path));
            ImageInputStream stream = new MemoryCacheImageInputStream(is);
            stream.setByteOrder(ByteOrder.BIG_ENDIAN);
            file.addProperty("size", new HexValue(file.getLength()));
            file.addProperty("filename", path.toAbsolutePath().toString());
            HighlightGroups highlights = new HighlightGroups();
            file.addProperty("highlights", highlights);

            readHeader(stream, file, highlights);
            extractBlocks(stream, file, out);
            scrollPlane(stream, highlights, file, out);

            return file;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void scrollPlane(ImageInputStream stream, HighlightGroups highlights, Block file, Path out) throws IOException {
        List<HexValue> blockOffsets = file.getObject("header", "block_offsets");
        int[] palette = readPalette(stream, highlights, blockOffsets.get(0).getValue());
        List<BufferedImage> tiles = readCells(stream, highlights, blockOffsets.get(1).getValue(), palette);
        writeTiles(out, tiles);
        List<BufferedImage> pages = readPages(stream, highlights, blockOffsets.get(2).getValue(), tiles);
        int pageNo = 0;
        for (BufferedImage image : pages) {
            ImageIO.write(image, "PNG", out.resolve("page"+pageNo+".png").toFile());
        }
    }

    private List<BufferedImage> readPages(ImageInputStream stream, HighlightGroups highlights, int offset, List<BufferedImage> cells) throws IOException {
        stream.seek(offset);
        DecompressedStream decompressor = new DecompressedStream(stream);
        highlights.addRange("page", offset, (int) (stream.getStreamPosition() - offset));
        ImageInputStream decompressedStream = decompressor.toStream();
        int numPages = decompressor.getSize() / (64*64*2);

        List<BufferedImage> pages = new ArrayList<>();
        for (int p = 0;  p < numPages; p++) {
            BufferedImage pageImage = new BufferedImage(64*8, 64*8, BufferedImage.TYPE_INT_RGB);
            Graphics2D pageGraphics = pageImage.createGraphics();
            pageGraphics.setColor(Color.CYAN.darker().darker().darker());
            for (int i = 0; i < 64*64; i++) {
                int x = i % 64;
                int y = i / 64;
                int pattern = decompressedStream.readUnsignedShort();
                int character = (pattern >> 1) & 0x7ff; // 12 bits character number
                int msb = pattern >> 12;
                if (msb > 0) {
                    System.out.println("msb == "+msb+" @ "+i);
                }
                BufferedImage cell = cells.get(character);
                pageGraphics.drawImage(cell, x*8, y*8, null);
            }
            pageGraphics.dispose();
            pages.add(pageImage);
        }
        return pages;
    }

    private void writeTiles(Path out, List<BufferedImage> tiles) throws IOException {
        int imageWidth = 352;
        int columns = imageWidth / 8;
        int rows = tiles.size() / columns;
        int imageHeight = rows * 8;
        BufferedImage tilesImage = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = tilesImage.createGraphics();
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < columns; x++) {
                int index = y * columns + x;
                if (index < tiles.size()) {
                    graphics.drawImage(tiles.get(index), x*8, y*8, null);
                }
            }
        }
        ImageIO.write(tilesImage, "PNG", out.resolve("tiles.png").toFile());
    }

    private List<BufferedImage> readCells(ImageInputStream stream, HighlightGroups highlights, int offset, int[] palette) throws IOException {
        stream.seek(offset);
        DecompressedStream decompressor = new DecompressedStream(stream);
        highlights.addRange("cells", offset, (int) (stream.getStreamPosition() - offset));
        ImageInputStream decompressedStream = decompressor.toStream();
        int length = decompressor.getSize();
        int numCells = length / (8 * 8);
        List<BufferedImage> images = new ArrayList<>();
        for (int i = 0; i < numCells;i++) {
            BufferedImage image = new BufferedImage(8,8,BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < 8; y++) {
                for (int x = 0; x < 8; x++) {
                    image.setRGB(x,y,palette[decompressedStream.readUnsignedByte()]);
                }
            }
            images.add(image);
        }
        return images;
    }

    private int[] readPalette(ImageInputStream stream, HighlightGroups highlights, int offset) throws IOException {
        stream.seek(offset);
        ImageInputStream decompressedStream = new DecompressedStream(stream).toStream();
        highlights.addRange("palette", offset, (int) (stream.getStreamPosition() - offset));
        int[] palette = new int[0x100];
        for (int i = 0; i < palette.length; i++) {
            palette[i] = Sf3Util.rgb16ToRgb24(decompressedStream.readShort());
        }
        return palette;
    }

    private void extractBlocks(ImageInputStream stream, Block file, Path out) throws IOException {
        List<HexValue> blockOffsets = file.getObject("header", "block_offsets");
        for (int i = 0; i < 3; i++) {
            stream.seek(blockOffsets.get(i).getValue());
            DecompressedStream decompressedStream = new DecompressedStream(stream);
            byte[] result = decompressedStream.getResult();
            Files.write(out.resolve("part"+i+".decompresssed"), result);
        }
    }

    private Block readHeader(ImageInputStream stream, Block file, HighlightGroups highlights) throws IOException {
        Block header = file.createBlock("header", (int) stream.getStreamPosition(), 0);
        List<HexValue> blockOffsets = header.addProperty("block_offsets", new ArrayList<HexValue>());
        for (int i = 0; i < 16; i++) {
            int offset = stream.readInt();
            if (offset != 0) {
                int relativeOffset = offset - 0x2e0000;
                blockOffsets.add(new HexValue(relativeOffset));
                highlights.addPointer("header_offset" + i, (int) (stream.getStreamPosition() - 4), relativeOffset);
            }
        }

        Block stuff = file.createBlock("stuff", (int) stream.getStreamPosition(), 0);
        stuff.addProperty("offset", new HexValue(stuff.getStart()));
        for (int i = 0; i < 20; i++) {
            int value = stream.readUnsignedShort();
            stuff.addProperty(String.format("stuff[%d]", i), new HexValue(value));
            if (value == 0xffff)
                break;
        }

        Block bitfields = file.createBlock("bitfields", (int) stream.getStreamPosition(), 0);
        bitfields.addProperty("offset", new HexValue(bitfields.getStart()));
        for (int i = 0; i < 47; i++) {
            int value = stream.readInt();
            bitfields.addProperty(String.format("bitfields[%d]", i), new HexValue(value));
        }

        for (int b = 0; b < 4; b++) {
            readBlock(stream, file, b);
        }

        for (int b = 4; b < 8; b++) {
            readBlock(stream, file, b);
            file.addProperty("padding"+b+"a", new HexValue(stream.readInt()));
            file.addProperty("padding"+b+"b", new HexValue(stream.readInt()));
        }


        return file;
    }

    private void readBlock(ImageInputStream stream, Block file, int b) throws IOException {
        Block block = file.createBlock(String.format("block[%d]", b), (int) stream.getStreamPosition(), 0);
        block.addProperty("start", new HexValue((int) stream.getStreamPosition()));
        block.addProperty("value1", new HexValue(stream.readUnsignedShort()));
        block.addProperty("null1", new HexValue(stream.readUnsignedShort()));
        block.addProperty("value2", new HexValue(stream.readUnsignedShort()));
        block.addProperty("foo1", new HexValue(stream.readUnsignedShort()));
        block.addProperty("foo2", new HexValue(stream.readUnsignedShort()));
        block.addProperty("value3", new HexValue(stream.readUnsignedShort()));

        List<Object> padding = new ArrayList<>();
        for (int p = 0; p < 5; p++) {
            padding.add(new HexValue(stream.readInt()));
        }
        block.addProperty("padding", padding);

        block.addProperty("end", new HexValue((int) stream.getStreamPosition()));
    }

}
