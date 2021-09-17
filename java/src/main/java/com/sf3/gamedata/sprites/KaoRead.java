package com.sf3.gamedata.sprites;

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
import java.util.List;

public class KaoRead {
    public static void main(String[] args) throws IOException {
        String basePath = System.getProperty("user.home") + "/project/games/shiningforce3/data/disk/dat";
        String outPath = System.getProperty("user.home") + "/project/games/shiningforce3/data/faces";

        String file = "kao001";
        try {
            Path path = Paths.get(basePath, file + ".dat");
            Path out = Paths.get(outPath, file);
            Files.createDirectories(out);
            Block chrFile = new KaoRead().readFile(path, out, file);
            Files.write(out.resolve(file + ".json"), Utils.toPrettyFormat(chrFile.toString()).getBytes(StandardCharsets.UTF_8));
            System.out.println("written " + file + ".json");
        } catch (Exception e) {
            System.out.println(file + " => " + e.toString());
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

            readChunk(stream, file, out, highlights);

            return file;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void readChunk(ImageInputStream stream, Block file, Path out, HighlightGroups highlights) throws IOException {
        DecompressedStream decompressedStream1 = new DecompressedStream(stream);
        ImageInputStream decompressedStream = decompressedStream1.toStream();
        int length = decompressedStream1.getSize();

        int width = decompressedStream.readUnsignedShort();
        int height = decompressedStream.readUnsignedShort();
        List<HexValue> values = file.addProperty("values", new ArrayList<HexValue>());
        for (int i = 0; i < 0xf; i++) {
            values.add(new HexValue(decompressedStream.readUnsignedShort()));
        }
        int[] palette = readPalette(decompressedStream);
        BufferedImage image = readBufferedImage(decompressedStream, palette, width, height);
        ImageIO.write(image, "PNG", out.resolve("chunk1.png").toFile());

        BufferedImage image2 = readBufferedImage(decompressedStream, palette, 0x20, 30);
        ImageIO.write(image2, "PNG", out.resolve("chunk2.png").toFile());

        BufferedImage image3 = readBufferedImage(decompressedStream, palette, 0x18, 24);
        ImageIO.write(image3, "PNG", out.resolve("chunk3.png").toFile());

        file.addProperty("length", new HexValue(length));
        file.addProperty("currentOffset", new HexValue((int) decompressedStream.getStreamPosition()));
    }

    public BufferedImage readBufferedImage(ImageInputStream stream, int[] palette, int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.setRGB(x,y,palette[stream.readUnsignedByte()]);
            }
        }
        return image;
    }

    private List<BufferedImage> readImages(ImageInputStream stream, Path out, HighlightGroups highlights, int[] palette, List<Integer> offsets) throws IOException {
        List<BufferedImage> result = new ArrayList<>();
        for (int offset : offsets) {
            stream.seek(offset);
            ImageInputStream decompressedStream = new DecompressedStream(stream).toStream();
            BufferedImage image = new BufferedImage(32, 32, BufferedImage.TYPE_INT_RGB);
            for (int i = 0; i < 32 * 32; i++) {
                int x = i % 32;
                int y = i / 32;
                int color = palette[decompressedStream.readUnsignedByte()];
                image.setRGB(x, y, color);
            }
            result.add(image);
        }
        return result;
    }

    private int[] readPalette(ImageInputStream stream) throws IOException {
        int[] palette = new int[0x100];
        for (int i = 0; i < palette.length; i++) {
            palette[i] = Sf3Util.rgb16ToRgb24(stream.readShort());
        }
        return palette;
    }

    private List<Integer> readHeader(ImageInputStream stream, Block file, HighlightGroups highlights) throws IOException {
        Block header = file.createBlock("header");
        List<Integer> imageOffsets = new ArrayList<>();
        while (true) {
            int offset = stream.readInt();
            if (offset == -1) {
                break;
            }
            imageOffsets.add(offset);
            highlights.addPointer("sprite", (int) (stream.getStreamPosition() - 4), offset);
        }
        return imageOffsets;
    }
}
