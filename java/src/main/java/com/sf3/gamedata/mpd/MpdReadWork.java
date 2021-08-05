package com.sf3.gamedata.mpd;

import com.sf3.binaryview.HighlightGroups;
import com.sf3.gamedata.utils.Block;
import com.sf3.gamedata.utils.HexValue;
import com.sf3.util.ByteArrayImageInputStream;
import com.sf3.util.Sf3Util;
import com.sf3.util.Utils;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/** contains all stuff which is work in progress. Already known stuff is in {@link MpdRead}. */
public class MpdReadWork extends MpdRead {
    public static void main(String[] args) throws IOException {
        String basePath = System.getProperty("user.home")+"/project/games/shiningforce3/data/disk/mpd";
        String outPath = System.getProperty("user.home")+"/project/games/shiningforce3/data/maps/";
//        String file = "void";
//        String file = "tesmap";
//        String file = "bal_1";
//        String file = "sara02";
//        String file =  "ship2";


//        List<String> files = Files.list(Paths.get(basePath))
//                .filter(path -> Files.isRegularFile(path))
//                .map(path -> path.getFileName())
//                .map((name -> name.toString()))
//                .map(name -> name.replace(".mpd", ""))
//                .collect(Collectors.toList());
        List<String> files = Arrays.asList(
                "chou00",
                "fed06",
                "field",
                "furain",
                "gdi5",
                "gdi"
        );
        for (String file : files) {
            try {
                Path path = Paths.get(basePath, file + ".mpd");

                Block mpdFile = new MpdReadWork().readFile(path, Paths.get(outPath, file));
                Files.writeString(Paths.get(outPath, file, file + ".json"), Utils.toPrettyFormat(mpdFile.toString()));
                System.out.println("done " + file);

                decodeScrollPane(outPath, file, mpdFile);
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println(String.format("cannot decode file %s: %s", file, e.getMessage()));
            }
        }
    }

    private static void decodeScrollPane(String outPath, String file, Block mpdFile) throws IOException {
        if (file.equals("sara02")) {
            // read scroll screen cells
            List<List<Integer>> palettes = mpdFile.getBlock("header").getObject("palettes");
            List<Integer> palette = palettes.get(0);

            List<BufferedImage> images = new ArrayList<>();
            images.addAll(readScrollCells(palette, Paths.get(outPath, file, "sara02.mpd.other0.decompressed")));
            images.addAll(readScrollCells(palette, Paths.get(outPath, file, "sara02.mpd.other1.decompressed")));
            System.out.println("scroll screen cells: "+new HexValue(images.size()));
            // read pattern name data aka scroll page
            byte[] patternData = Files.readAllBytes(Paths.get(outPath, file, "sara02.mpd.other2.decompressed"));
            ImageInputStream patternStream = new ByteArrayImageInputStream(patternData);
            patternStream.setByteOrder(ByteOrder.BIG_ENDIAN);
            for (int page = 0; page < patternData.length / (64*64*2); page++) {
                // note: scroll pages are always 64x64
                BufferedImage pageImage = new BufferedImage(64 * 8, 64 * 8, BufferedImage.TYPE_INT_RGB);
                Graphics2D pageGraphics = pageImage.createGraphics();
                for (int h = 0; h < 64; h++) {
                    for (int w = 0; w < 64; w++) {
                        int pattern = patternStream.readUnsignedShort();
                        int character = pattern & 0x3ff; // 10 bits character number
                        pageGraphics.drawImage(images.get(character/2), w * 8, h * 8, null);
                    }
                }
                pageGraphics.dispose();
                ImageIO.write(pageImage, "png", Paths.get(outPath, file, "sara02.mpd.other2.scroll"+page+".png").toFile());
            }
        }
    }

    private static List<BufferedImage> readScrollCells(List<Integer> palette, Path path1) throws IOException {
        List<BufferedImage> images = new ArrayList<>();
        byte[] data = Files.readAllBytes(path1);
        ImageInputStream stream = new ByteArrayImageInputStream(data);
        stream.setByteOrder(ByteOrder.BIG_ENDIAN);
        for (int cel = 0; cel < data.length / (8*8); cel++) {
            // cells are always 8x8 dots (aka pixels)
            BufferedImage image = new BufferedImage(8,8,BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < 8; y++) {
                for (int x = 0; x < 8; x++) {
                    int col = stream.read();
                    image.setRGB(x,y, palette.get(col));
                }
            }
            images.add(image);
        }
        return images;
    }

    protected Block readHeader(HighlightGroups highlightGroups, Block file, ImageInputStream stream) throws IOException {
        Block header = file.createBlock("header", 0, 0x4);
        // note: mpd files will be placed ~ 0x00290000
        // @see "https://forums.shiningforcecentral.com/viewtopic.php?f=10&t=45857&sid=777c738b5498cb68c3e2f0a5e8c6980b#p804838"

        // find all pointers up to first known next chunk: 0x2000
        Map<HexValue, HexValue> pointers = new TreeMap<>();
        for (int i = 0; i < 0x2000/4;i++) {
            int value = stream.readInt();
            if (value > 0x29_0000 && value < 0x30_0000) {
                pointers.put(new HexValue(value-0x29_0000), new HexValue(1));
            }
        }
        // calculate difference from one pointer to next
        Map.Entry<HexValue, HexValue> lastEntry = null;
        for (Map.Entry<HexValue, HexValue> entry : pointers.entrySet()) {
            if (lastEntry != null) {
                lastEntry.setValue(new HexValue(entry.getKey().getValue()-lastEntry.getKey().getValue()));
            }
            lastEntry = entry;
        }

        stream.seek(0);
        highlightGroups.addGroup("file_header", 0x009933);
        highlightGroups.addGroup("strange_header_pointers", 0x66cc99);
        highlightGroups.addGroup("header_ascending_value", 0x0066ff);
        int subHeader = stream.readInt() - MAP_MEMORY_OFFSET;
        highlightGroups.addPointer("file_header", 0, subHeader);
        header.addProperty("offset_sub_header", new HexValue(subHeader));
        stream.seek(subHeader);
        int subHeader2 = stream.readInt() - MAP_MEMORY_OFFSET;
        highlightGroups.addPointer("file_header", subHeader, subHeader2);
        header.addProperty("offset_sub_header2", new HexValue(subHeader2));
        stream.seek(subHeader2);

        // this might be some count. second byte (lsb) is always 0x3 (might also be a bitfield)
        // first byte (msb) can be map id (does not match https://forums.shiningforcecentral.com/viewtopic.php?f=10&t=45699&p=800656)
        header.addProperty("short[0]", new HexValue(stream.readUnsignedShort()));
        header.addProperty("zero[0]", new HexValue(stream.readUnsignedShort()));
        // always 0x40 bytes (0x20 )
        header.addProperty("offset[0]", new HexValue(readPointer(stream, highlightGroups, "header_offset0")));
        // always 4 bytes
        header.addProperty("offset[1]", new HexValue(readPointer(stream, highlightGroups, "header_offset1")));
        // either 4 0r 0x40 bytes
        header.addProperty("offset[2]", new HexValue(readPointer(stream, highlightGroups, "header")));
        header.addProperty("short[1]", new HexValue(stream.readUnsignedShort()));
        header.addProperty("zero[1]", new HexValue(stream.readUnsignedShort()));

        // some sublists which seems to relate to texture indices or groups.
        // note: looks like missing (moveable/interactible) objects will won't have stuff at offset_4 too
        header.addProperty("offset_4", new HexValue(readPointer(stream, highlightGroups, "header")));
        // list of texture animation groups
        header.addProperty("offset_texture_animations", new HexValue(readPointer(stream, highlightGroups, "textureAnimations")));

        header.addProperty("offset_6", new HexValue(readPointer(stream, highlightGroups, "header")));
        header.addProperty("offset_7", new HexValue(readPointer(stream, highlightGroups, "header")));
        // at most 3 moveable or interactable objects (chests, barrel)
        header.addProperty("offset_object_1", new HexValue(readPointer(stream, highlightGroups, "header_objects")));
        header.addProperty("offset_object_2", new HexValue(readPointer(stream, highlightGroups, "header_objects")));
        header.addProperty("offset_object_3", new HexValue(readPointer(stream, highlightGroups, "header_objects")));

        header.addProperty("value_01", new HexValue(stream.readInt()));
        header.addProperty("value_02", new HexValue(stream.readInt()));
        // looks like these are animated texture indices.
        header.addProperty("offset_11", new HexValue(readPointer(stream, highlightGroups, "header")));
        header.addProperty("offset_palette1", new HexValue(readPointer(stream, highlightGroups, "palette1")));
        header.addProperty("offset_palette2", new HexValue(readPointer(stream, highlightGroups, "palette2")));
        header.addProperty("value_03", new HexValue(stream.readInt()));
        header.addProperty("value_04", new HexValue(stream.readInt()));
        header.addProperty("value_05", new HexValue(stream.readInt()));
        // ie "0x1860000", => offset 60000 in Registers?
        header.addProperty("value_06", new HexValue(stream.readInt()));
        // might also be 2 "rows" of 4 shorts each
        header.addProperty("offset_14", new HexValue(readPointer(stream, highlightGroups, "header")));

        int streamPosition = (int) stream.getStreamPosition();
        header.addProperty("current_file_offset1", new HexValue(streamPosition));
        header.addProperty("current_relative_file_offset1", new HexValue(streamPosition - subHeader2));

        highlightGroups.addRange("file_header", subHeader2, streamPosition-subHeader2);
        highlightGroups.addRange("header_ascending_value", header.getInt("offset[0]"), 0x40);

        header.addProperty("all_pointers", pointers);

        Block meshBlock = header.createBlock("objects", 0, 0);

        readTextureAnimationDescription(highlightGroups, stream, header);

        highlightGroups.addGroup("stuff_at_offset_8", 0xFFFF00);
        highlightGroups.addGroup("stuff_at_offset_8_items", 0xCCCCFF);
        for (int i = 1; i <= 3; i++) {
            int offset = header.getInt("offset_object_" + i);
            if (offset != 0) {
                meshBlock.addBlock(readHeaderObjects(highlightGroups, stream, header, offset, i));
            }
        }

        readStuffAtOffset0(highlightGroups, stream, header);
        readStuffAtOffset1(highlightGroups, stream, header);
        readStuffAtOffset2(highlightGroups, stream, header);
        // @see 4.2 Scroll Screen Structure. This might be a scroll screen configuration
        readStuffAtOffset4(highlightGroups, stream, header);
        readStuffAtOffset11(highlightGroups, stream, header);
        readStuffAtOffset14(highlightGroups, stream, header);


        List<List<Integer>> palettes = new ArrayList<List<Integer>>();
        palettes.add(readPalette(highlightGroups, stream, header.getInt("offset_palette1"), header.getInt("offset_palette2")));
        palettes.add(readPalette(highlightGroups, stream, header.getInt("offset_palette2"), header.getInt("offset_sub_header2")));
        header.addProperty("palettes", palettes);
        return header;
    }

    private void readStuffAtOffset4(HighlightGroups highlightGroups, ImageInputStream stream, Block header) throws IOException {
        int offset = header.getInt("offset_4");
        Block stuff = header.createBlock("stuff_at_offset4", offset, 0);
        stream.seek(offset);

        for (int i = 0;;i++)  {
            int value = stream.readInt();
            if (value == -1) {
                break;
            }
            Block item = stuff.createBlock("item[" + i + "]", 0, 0);
            item.addProperty("value1", new HexValue(value));
            item.addProperty("offset1", new HexValue(readPointer(stream, highlightGroups, "header_offset4")));
            item.addProperty("offset2", new HexValue(readPointer(stream, highlightGroups, "header_offset4")));
            item.addProperty("value2", new HexValue(stream.readInt()));
        }
        // read list behind offset1 and offset2
        for (Block item : stuff.valuesStream(Block.class).collect(Collectors.toList())) {
            item.addProperty("list_offset_1", readList(stream, item, "offset1"));
            item.addProperty("list_offset_2", readList(stream, item, "offset2"));
        }

        highlightGroups.addRange("header_offset4", offset, (int) (stream.getStreamPosition() - offset));
    }

    private List<HexValue> readList(ImageInputStream stream, Block item, String offset11) throws IOException {
        int offset1 = item.getInt(offset11);
        stream.seek(offset1);
        List<HexValue> values = new ArrayList<>();
        while (true) {
            int value = stream.readShort();
            if (value == -1) {
                break;
            }
            values.add(new HexValue(value));
        }
        return values;
    }

    private void readStuffAtOffset14(HighlightGroups highlightGroups, ImageInputStream stream, Block header) throws IOException {
        int offset = header.getInt("offset_14");
        Block stuff = header.createBlock("stuff_at_offset14", offset, 0x10);
        stream.seek(offset);
        stuff.addProperty("foo1", new HexValue(stream.readShort()));
        stuff.addProperty("foo2", new HexValue(stream.readShort()));
        stuff.addProperty("foo3", new HexValue(stream.readShort()));
        stuff.addProperty("foo4", new HexValue(stream.readShort()));

        stuff.addProperty("foo5", new HexValue(stream.readShort()));
        stuff.addProperty("foo6", new HexValue(stream.readShort()));
        stuff.addProperty("foo7", new HexValue(stream.readShort()));
        stuff.addProperty("foo8", new HexValue(stream.readShort()));
        highlightGroups.addRange("header_offset14", offset, 0x10);
    }

    private void readStuffAtOffset11(HighlightGroups highlightGroups, ImageInputStream stream, Block header) throws IOException {
        int offset = header.getInt("offset_11");
        Block stuff = header.createBlock("stuff_at_offset11", offset, 0);
        stream.seek(offset);
        List<HexValue> values = new ArrayList<HexValue>();
        // only read some values when the next area is not the same as this area (so this areas size is zero)
        while (true) {
            int value = stream.readUnsignedShort();
            if (value == 0xffff) {
                break;
            }
            values.add(new HexValue(value));
        }
        stuff.addProperty("size", values.size() * 2);
        stuff.addProperty("count", values.size());
        stuff.addProperty("values", values);
        highlightGroups.addRange("header_offset11", offset, (values.size()+1) * 2);
    }

    private void readStuffAtOffset0(HighlightGroups highlightGroups, ImageInputStream stream, Block header) throws IOException {
        int offset0 = header.getInt("offset[0]");
        Block stuff = header.createBlock("stuff_at_offset0", offset0, 0x40);
        stream.seek(offset0);
        List<HexValue> values = IntStream.range(0,0x20)
                .map(i -> Sf3Util.readUnsignedShort(stream))
                .mapToObj(HexValue::new)
                .collect(Collectors.toList());
        stuff.addProperty("count: ", values.size());
        stuff.addProperty("values", values);
        highlightGroups.addRange("header_offset0", offset0, 0x40);
    }

    private void readStuffAtOffset1(HighlightGroups highlightGroups, ImageInputStream stream, Block header) throws IOException {
        int offset1 = header.getInt("offset[1]");
        Block stuff = header.createBlock("stuff_at_offset1", offset1, 0x4);
        stream.seek(offset1);
        stuff.addProperty("value", new HexValue(Sf3Util.readInt(stream)));
        highlightGroups.addRange("header_offset1", offset1, 0x4);
    }

    private void readStuffAtOffset2(HighlightGroups highlightGroups, ImageInputStream stream, Block header) throws IOException {
        int offset2 = header.getInt("offset[2]");
        int offset4 = header.getInt("offset_4");
        Block stuff = header.createBlock("stuff_at_offset2", offset2, 0x4);
        stream.seek(offset2);
        List<HexValue> values = new ArrayList<HexValue>();
        // only read some values when the next area is not the same as this area (so this areas size is zero)
        if (offset2 < offset4) {
            List<HexValue> additionalValues = IntStream.range(0,((offset4-offset2) / 4)-1)
                    .map(i -> Sf3Util.readInt(stream))
                    .mapToObj(HexValue::new)
                    .collect(Collectors.toList());
            values.addAll(additionalValues);
        }
        stuff.addProperty("size", new HexValue(values.size() * 4));
        stuff.addProperty("count", new HexValue(values.size()));
        stuff.addProperty("values", values);
        highlightGroups.addRange("header_offset2", offset2, values.size() * 4);
    }


}
