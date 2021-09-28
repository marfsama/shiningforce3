package com.sf3.gamedata.sprites;

import com.sf3.binaryview.HighlightGroups;
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
import java.util.List;
import java.util.stream.Collectors;

public class ItemsRead {
    public static void main(String[] args) throws IOException {
        String basePath = System.getProperty("user.home") + "/project/games/shiningforce3/data/disk/dat";
        String outPath = System.getProperty("user.home") + "/project/games/shiningforce3/data/";

        String file = "item_cg";
        try {
            Path path = Paths.get(basePath, file + ".dat");
            Path out = Paths.get(outPath, file);
            Files.createDirectories(out);
            Block itemsFile = new ItemsRead().readFile(path, out, file);
            Files.write(out.resolve(file + ".json"), Utils.toPrettyFormat(itemsFile.toString()).getBytes(StandardCharsets.UTF_8));
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

            readHeader(stream, file, out, highlights);

            return file;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void readHeader(ImageInputStream stream, Block file, Path out, HighlightGroups highlights) throws IOException {
        int commandOffset = stream.readUnsignedShort();
        highlights.addPointer("command_offset", 0, commandOffset);

        for (int i = 0; i < 20; i++) {
            Block itemBlock = file.createBlock("[" + i + "]");
            itemBlock.addProperty("offset", new HexValue(stream.readUnsignedShort()));
            itemBlock.addProperty("width", new HexValue(stream.readUnsignedShort()));
            itemBlock.addProperty("height", new HexValue(stream.readUnsignedShort()));
            itemBlock.addProperty("foo1a", new HexValue(stream.readUnsignedShort()));
            itemBlock.addProperty("foo1b", new HexValue(stream.readUnsignedShort()));
            itemBlock.addProperty("foo2a", new HexValue(stream.readUnsignedShort()));
            itemBlock.addProperty("foo2b", new HexValue(stream.readUnsignedShort()));
            itemBlock.addProperty("foo3a", new HexValue(stream.readUnsignedShort()));
            itemBlock.addProperty("foo3b", new HexValue(stream.readUnsignedShort()));
            itemBlock.addProperty("foo4a", new HexValue(stream.readUnsignedShort()));
            itemBlock.addProperty("foo4b", new HexValue(stream.readUnsignedShort()));
            itemBlock.addProperty("foo5", new HexValue(stream.readUnsignedShort()));

        }

        byte[] colorData = new byte[(int) (commandOffset-stream.getStreamPosition())];
        stream.readFully(colorData);
        stream.seek(commandOffset);

        List<Integer> colors = SpriteUtils.decompressImage(colorData, stream);
        int width = 64;
        int height = colors.size() / width;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = y * width + x;
                if (index < colors.size()) {
                    image.setRGB(x, y, Sf3Util.rgb16ToRgb24(colors.get(index)));
                }
            }
        }
        ImageIO.write(image, "PNG", out.resolve("items.png").toFile());

    }
}
