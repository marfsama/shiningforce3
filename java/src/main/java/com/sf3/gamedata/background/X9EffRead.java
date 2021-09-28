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

public class X9EffRead {
    public static void main(String[] args) {
        String basePath = System.getProperty("user.home")+"/project/games/shiningforce3/data/disk/bin/x9";
        String outPath = System.getProperty("user.home") + "/project/games/shiningforce3/data";

        List<String> filenames = Arrays.asList("x9effdat.bin");
        for (String fileName : filenames) {
            Path path = Paths.get(basePath, fileName);

            try (InputStream inputStream = Files.newInputStream(path)) {
                ImageInputStream stream = new MemoryCacheImageInputStream(inputStream);
                stream.setByteOrder(ByteOrder.BIG_ENDIAN);
                Path out = Paths.get(outPath, fileName);
                Files.createDirectories(out);

                Block file = new X9EffRead().readFile(path, out, fileName);
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

            List<Integer> offsets = readHeader(stream, file, highlights);
            extractBlock(stream, out, offsets.get(0), 0);
            extractBlock(stream, out, offsets.get(1), 1);
            extractBlock(stream, out, offsets.get(2), 2);
            extractBlock(stream, out, offsets.get(4), 4);

            int[] palette = readPalette(stream, highlights, offsets.get(3));
            readImage(stream, out, highlights, offsets.get(0), palette, 80, "image1");
            readImage(stream, out, highlights, offsets.get(1), null, 16, "image2");
            readImage(stream, out, highlights, offsets.get(2), null, 16, "image3");
            readImage(stream, out, highlights, offsets.get(4), palette, 32, "image4");

            return file;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void readImage(ImageInputStream stream, Path out, HighlightGroups highlights, int offset, int[] palette, int width, String name) throws IOException {
        stream.seek(offset);
        DecompressedStream decompressor = new DecompressedStream(stream);
        ImageInputStream decompressedStream = decompressor.toStream();
        highlights.addRange(name, offset, (int) (stream.getStreamPosition() - offset));
        int height = decompressor.getSize() / (width * (palette == null ? 2 : 1));
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (palette != null) {
                    image.setRGB(x, y, palette[decompressedStream.readUnsignedByte()]);
                }
                else {
                    image.setRGB(x, y, Sf3Util.rgb16ToRgb24(decompressedStream.readShort()));
                }
            }
        }
        ImageIO.write(image, "PNG", out.resolve(name+".png").toFile());
    }

    private int[] readPalette(ImageInputStream stream, HighlightGroups highlights, int offset) throws IOException {
        stream.seek(offset);
        int[] palette = new int[0x100];
        for (int i = 0; i < palette.length; i++) {
            palette[i] = Sf3Util.rgb16ToRgb24(stream.readShort());
        }
        highlights.addRange("palette", offset, (int) (stream.getStreamPosition() - offset));
        return palette;
    }


    public void extractBlock(ImageInputStream stream, Path out, int offset, int num) throws IOException {
        stream.seek(offset);
        DecompressedStream decompressedStream = new DecompressedStream(stream);
        byte[] result = decompressedStream.getResult();
        Files.write(out.resolve("part"+num+".decompresssed"), result);

    }

    private List<Integer> readHeader(ImageInputStream stream, Block file, HighlightGroups highlights) throws IOException {
        Block header = file.createBlock("header");
        List<Integer> offsets = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Block item = header.createBlock("[" + i + "]");
            int offset = stream.readInt() - 0x60a0000;
            offsets.add(offset);
            highlights.addPointer("offset", (int) (stream.getStreamPosition()-4), offset);
            item.addProperty("offset", new HexValue(offset));
            item.addProperty("null1", new HexValue(stream.readInt()));
            item.addProperty("null2", new HexValue(stream.readInt()));
        }
        highlights.addRange("header", 0, (int) stream.getStreamPosition());
        return offsets;
    }
}
