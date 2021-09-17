package com.sf3.gamedata.sprites;


import com.sf3.binaryview.HighlightGroups;
import com.sf3.gamedata.mpd.DecompressedStream;
import com.sf3.gamedata.utils.Block;
import com.sf3.gamedata.utils.HexValue;
import com.sf3.util.Sf3Util;
import com.sf3.util.Utils;

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

public class FacesRead {
    public static void main(String[] args) throws IOException {
        String basePath = System.getProperty("user.home") + "/project/games/shiningforce3/data/disk/dat";
        String outPath = System.getProperty("user.home") + "/project/games/shiningforce3/data/faces";

        String file = "face32";
        try {
            Path path = Paths.get(basePath, file + ".dat");
            Path out = Paths.get(outPath, file);
            Files.createDirectories(out);
            Block faceFile = new FacesRead().readFile(path, out, file);
            Files.write(out.resolve(file + ".json"), Utils.toPrettyFormat(faceFile.toString()).getBytes(StandardCharsets.UTF_8));
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

            List<Integer> offsets = readHeader(stream, file, highlights);
            int[] palette = readPalette(stream, highlights);
            List<BufferedImage> images = readImages(stream, out, highlights, palette, offsets);

            SpriteUtils.writeSpriteSheet(out, "faces32.png", 32, 32, images);

            return file;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
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

    private int[] readPalette(ImageInputStream stream, HighlightGroups highlights) throws IOException {
        stream.seek(0x100);
        highlights.addRange("palette", 0x100, 0x200);
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
