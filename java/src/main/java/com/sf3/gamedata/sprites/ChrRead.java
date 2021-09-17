package com.sf3.gamedata.sprites;

import com.sf3.binaryview.HighlightGroups;
import com.sf3.gamedata.utils.Block;
import com.sf3.gamedata.utils.HexValue;
import com.sf3.util.ByteArrayImageInputStream;
import com.sf3.util.Sf3Util;
import com.sf3.util.Utils;
import lombok.Data;

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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


/** Read battle sprite files. */
public class ChrRead {

    public static void main(String[] args) throws IOException {
        String basePath = System.getProperty("user.home") + "/project/games/shiningforce3/data/disk/chr - town sprite";
        String outPath = System.getProperty("user.home") + "/project/games/shiningforce3/data/town";
//        List<String> files = Arrays.asList(
//                "xsara1"
//                "cbf02",
//                "cbf14",
//                "cbf10",
//                "cbe04",
//                "cbe05"
//        );
        List<String> files = Files.list(Paths.get(basePath))
                .filter(Files::isRegularFile)
                .map(Path::getFileName)
                .map((Path::toString))
                .map(name -> name.replace(".chr", ""))
                .sorted()
                .collect(Collectors.toList());

        for (String file : files) {
            try {
                Path path = Paths.get(basePath, file + ".chr");
                Path out = Paths.get(outPath, file);
                Files.createDirectories(out);
                Block chrFile = new ChrRead().readFile(path, out, file);
                Files.write(out.resolve(file + ".json"), Utils.toPrettyFormat(chrFile.toString()).getBytes(StandardCharsets.UTF_8));
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

            List<SpriteSheetHeader> spriteSheets = readHeader(stream, file);
            readAnimations(stream, file, spriteSheets, highlights);
            readSprites(stream, out, filename, file, spriteSheets, highlights);


            return file;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void readSprites(ImageInputStream stream, Path out, String filename, Block file, List<SpriteSheetHeader> spriteSheets, HighlightGroups highlights) throws IOException {
        for (SpriteSheetHeader spriteSheet : spriteSheets) {
            int spriteListOffset = spriteSheet.getSpritesOffset();
            Block spriteSheetBlock = file.createBlock("spriteSheet_" + spriteListOffset);
            List<Integer> offsets = new ArrayList<>();
            stream.seek(spriteListOffset);
            while (true) {
                int spriteOffset = stream.readInt();
                if (spriteOffset == 0) {
                    break;
                }
                offsets.add(spriteOffset);
            }
            highlights.addRange("sprite_offsets", spriteListOffset, offsets.size() * 4 + 4);
            List<BufferedImage> sprites = new ArrayList<>();
            for (int offset : offsets) {
                sprites.add(SpriteUtils.readSprite(stream, spriteSheetBlock, highlights, offset, spriteSheet.getWidth(), spriteSheet.getHeight()));
            }
            SpriteUtils.writeSpriteSheet(out, filename + "_" + spriteListOffset, spriteSheet.getWidth(), spriteSheet.getHeight(), sprites);
        }
    }

    private void readAnimations(ImageInputStream stream, Block file, List<SpriteSheetHeader> spriteSheets, HighlightGroups highlights) throws IOException {
        Block animationsBlock = file.createBlock("animations");

        for (SpriteSheetHeader spriteSheet : spriteSheets) {
            int animationsOffset = spriteSheet.getAnimationsOffset();
            Block spriteSheetBlock = animationsBlock.createBlock("sprite_sheet_"+animationsOffset);
            stream.seek(animationsOffset);
            List<HexValue> offsets = spriteSheetBlock.addProperty("offsets", new ArrayList<>());
            for (int i = 0; i < 0x10; i++) {
                int offset = stream.readInt();
                if (offset == 0) {
                    break;
                }
                offsets.add(new HexValue(offset));
            }
            highlights.addRange("animationsOffset", animationsOffset, 0x10 * 4);

            for (HexValue offset : offsets) {
                stream.seek(offset.getValue());
                List<AnimationFrame> frames = spriteSheetBlock.addProperty("frames_" + offset, new ArrayList<>());
                for (int i = 0; i < 0x10; i++) {
                    AnimationFrame frame = new AnimationFrame(stream);
                    frames.add(frame);
                    if (frame.getUnknown() == 0) {
                        // maybe stop if spriteIndex msb is set?
                        break;
                    }
                }
                highlights.addRange("animations", offset.getValue(), frames.size() * 4);
            }
        }
    }

    private List<SpriteSheetHeader> readHeader(ImageInputStream stream, Block file) throws IOException {
        Block header = file.createBlock("header");
        header.addProperty("count", stream.readShort());
        List<SpriteSheetHeader> spriteSheets = new ArrayList<>();
        for (int i = 0; i < 0x20; i++) {
            Block item = header.createBlock("item_" + i);
            SpriteSheetHeader spriteSheetHeader = new SpriteSheetHeader(stream);
            spriteSheets.add(spriteSheetHeader);
            item.addProperty("sprite_sheet", spriteSheetHeader);
            int spriteId = stream.readUnsignedShort();
            item.addProperty("sprite_id", new HexValue(spriteId));
            if (spriteId == 0xffff) {
                break;
            }
        }
        return spriteSheets;
    }
}
