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
import java.util.stream.Collectors;

public class KaoRead {
    public static void main(String[] args) throws IOException {
        String basePath = System.getProperty("user.home") + "/project/games/shiningforce3/data/disk/dat";
        String outPath = System.getProperty("user.home") + "/project/games/shiningforce3/data/faces";

        List<String> files = Files.list(Paths.get(basePath))
                .filter(Files::isRegularFile)
                .filter(file -> file.getFileName().toString().startsWith("kao"))
                .map(Path::getFileName)
                .map((Path::toString))
                .map(name -> name.replace(".dat", ""))
                .sorted()
                .collect(Collectors.toList());
        for (String file : files) {
            try {
                Path path = Paths.get(basePath, file + ".dat");
                Path out = Paths.get(outPath, file);
                Files.createDirectories(out);
                Block kaoFile = new KaoRead().readFile(path, out, file);
                Files.write(out.resolve(file + ".json"), Utils.toPrettyFormat(kaoFile.toString()).getBytes(StandardCharsets.UTF_8));
                System.out.println("written " + file + ".json");
            } catch (Exception e) {
                System.out.println(file + " => " + e.toString());
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

            int offset = 0;
            int num = 0;
            while (offset < file.getLength()) {
                stream.seek(offset);
                readFace(stream, file, out, num++, highlights);
                // skip to next full 0x800 boundary
                offset = ((((int) stream.getStreamPosition() + 0x7ff) / 0x800) * 0x800);
            }

            return file;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void readFace(ImageInputStream stream, Block file, Path out, int num, HighlightGroups highlights) throws IOException {
        ImageInputStream decompressedStream = new DecompressedStream(stream).toStream();

        Block faceBlock = file.createBlock("face"+num);
        int width = decompressedStream.readUnsignedShort();
        int height = decompressedStream.readUnsignedShort();
        // offsets to individual images
        List<HexValue> offsets = faceBlock.addProperty("offsets", new ArrayList<HexValue>());
        for (int i = 0; i < 0x9; i++) {
            offsets.add(new HexValue(decompressedStream.readUnsignedShort()));
        }
        int eyesWidth = decompressedStream.readUnsignedShort();
        int eyesHeight = decompressedStream.readUnsignedShort();
        int mouthWidth = decompressedStream.readUnsignedShort();
        int mouthHeight = decompressedStream.readUnsignedShort();
        int unknownWidth = decompressedStream.readUnsignedShort();
        int unknownHeight = decompressedStream.readUnsignedShort();
        faceBlock.addProperty("eyesWidth", eyesWidth);
        faceBlock.addProperty("eyesHeight", eyesHeight);
        faceBlock.addProperty("mouthWidth", mouthWidth);
        faceBlock.addProperty("mouthHeight", mouthHeight);
        faceBlock.addProperty("unknownWidth", new HexValue(unknownWidth));
        faceBlock.addProperty("unknownHeight", new HexValue(unknownHeight));

        int[] palette = readPalette(decompressedStream);
        BufferedImage image = readBufferedImage(decompressedStream, palette, width, height);
        ImageIO.write(image, "PNG", out.resolve(num+"face.png").toFile());

        BufferedImage image2 = readBufferedImage(decompressedStream, palette, eyesWidth, eyesHeight*3);
        ImageIO.write(image2, "PNG", out.resolve(num+"eyes.png").toFile());

        BufferedImage image3 = readBufferedImage(decompressedStream, palette, mouthWidth, mouthHeight*3);
        ImageIO.write(image3, "PNG", out.resolve(num+"mouth.png").toFile());
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

    private int[] readPalette(ImageInputStream stream) throws IOException {
        int[] palette = new int[0x100];
        for (int i = 0; i < palette.length; i++) {
            palette[i] = Sf3Util.rgb16ToRgb24(stream.readShort());
        }
        return palette;
    }

}
