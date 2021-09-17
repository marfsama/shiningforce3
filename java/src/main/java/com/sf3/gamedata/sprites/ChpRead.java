package com.sf3.gamedata.sprites;

import com.sf3.binaryview.HighlightGroups;
import com.sf3.gamedata.utils.Block;
import com.sf3.gamedata.utils.HexValue;
import com.sf3.util.ByteArrayImageInputStream;
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
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;


/** Read battle sprite files. */
public class ChpRead {

    public static void main(String[] args) throws IOException {
        String basePath = System.getProperty("user.home")+"/project/games/shiningforce3/data/disk/chp - battle sprite";
        String outPath = System.getProperty("user.home")+"/project/games/shiningforce3/data/character";
//        List<String> files = Arrays.asList(
//                "cbe00",
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
                .map(name -> name.replace(".chp", ""))
                .sorted()
                .collect(Collectors.toList());

        for (String file : files) {
            try {
                Path path = Paths.get(basePath, file + ".chp");
                Path out = Paths.get(outPath, file);
                Files.createDirectories(out);
                Block chpFile = new ChpRead().readFile(path, out, file);
                Files.write(out.resolve(file + ".json"), Utils.toPrettyFormat(chpFile.toString()).getBytes(StandardCharsets.UTF_8));
                System.out.println("written " + file + ".json");
            }
            catch (Exception e) {
                System.out.println(file+" => "+e.toString());
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
            HighlightGroups highlightGroups = new HighlightGroups();
            file.addProperty("highlights", highlightGroups);

            int offset = 0;
            while (offset < file.getLength()) {
                readSpriteSheet(stream, file, filename, out, highlightGroups, offset);
                // skip to next full 0x800 boundary
                offset = ((((int) stream.getStreamPosition() + 0x7ff) / 0x800) * 0x800);
            }

            return file;
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public HexValue readPointer(ImageInputStream stream, HighlightGroups highlights, String name, int relativeOffset) throws IOException {
        int offset = stream.readInt();
        if (offset > 0) {
            highlights.addPointer(name, (int) (stream.getStreamPosition() - 4), offset+relativeOffset);
        }
        return new HexValue(offset);
    }

    private void readSpriteSheet(ImageInputStream stream, Block file, String filename, Path out, HighlightGroups highlights, int offset) throws IOException {
        Block header = file.createBlock("sprite_sheet_"+new HexValue(offset));

        stream.seek(offset);
        int characterSheetNo = stream.readShort();
        if (characterSheetNo == -1) {
            return;
        }
        header.addProperty("character_sheet_no", new HexValue(characterSheetNo));
        int width = stream.readUnsignedShort();
        int height = stream.readUnsignedShort();
        header.addProperty("width", width);
        header.addProperty("height", height);
        header.addProperty("unknown1", new HexValue(stream.readUnsignedShort()));
        header.addProperty("unknown2", new HexValue(stream.readUnsignedShort()));
        header.addProperty("unknown3", new HexValue(stream.readUnsignedShort()));
        header.addProperty("unknown4", new HexValue(stream.readUnsignedShort()));
        header.addProperty("unknown5", new HexValue(stream.readUnsignedShort()));
        header.addProperty("sprite_offsets_offset", readPointer(stream, highlights, "sprite_offsets", offset));
        header.addProperty("animations_offsets", readPointer(stream, highlights, "animation", offset));
        header.addProperty("end_of_list", new HexValue(stream.readUnsignedShort()));

        highlights.addRange("header", offset, (int) stream.getStreamPosition() - offset);

        Block animationsBlock = header.createBlock("animations");
        int animationsOffset = header.getInt("animations_offsets");
        stream.seek((long) offset + animationsOffset);
        // read list of animation offsets. Always 0x10 entries, unused entries are zero
        List<HexValue> animationOffsets = animationsBlock.addProperty("offsets", new ArrayList<>());
        for (int i = 0; i < 0x10; i++) {
            HexValue animationOffset = readPointer(stream, highlights, "animation", offset);
            if (animationOffset.getValue() != 0) {
                animationOffsets.add(animationOffset);
            }
        }
        highlights.addRange("animation", offset + animationsOffset, (int) stream.getStreamPosition() - (offset+animationsOffset));

        // read animations
        for (HexValue animationOffset : animationOffsets) {
            stream.seek((long) offset + animationOffset.getValue());
            List<AnimationFrame> animationValues = animationsBlock.addProperty("animation_" + animationOffset, new ArrayList<>());
            for (int i = 0; i < 0x10; i++) {
                AnimationFrame frame = new AnimationFrame(stream);
                animationValues.add(frame);
                if (frame.getUnknown() == 0) {
                    // maybe: if spriteIndex msb is set?
                    break;
                }
            }
        }

        List<HexValue> offsets = header.addProperty("sprite_offset_list", new ArrayList<HexValue>());
        int spriteListOffset = header.getInt("sprite_offsets_offset");
        stream.seek((long) offset + spriteListOffset);
        while (true) {
            HexValue spriteOffset = new HexValue(stream.readInt());
            if (spriteOffset.getValue() == 0) {
                break;
            }
            offsets.add(spriteOffset);
        }
        highlights.addRange("sprite_offsets", spriteListOffset + offset, (int) stream.getStreamPosition() - (spriteListOffset + offset));


        List<BufferedImage> sprites = new ArrayList<>();
        for (HexValue spriteOffset : offsets) {
            sprites.add(SpriteUtils.readSprite(stream, header, highlights, offset + spriteOffset.getValue(), header.getInt("width"), header.getInt("height")));
        }

        SpriteUtils.writeSpriteSheet(out, filename+"_"+offset, width, height, sprites);
    }



}
