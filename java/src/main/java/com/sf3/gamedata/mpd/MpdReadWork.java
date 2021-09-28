package com.sf3.gamedata.mpd;

import com.sf3.binaryview.HighlightGroups;
import com.sf3.gamedata.sgl.Angle;
import com.sf3.gamedata.sgl.Fixed;
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
        String basePath = System.getProperty("user.home")+"/project/games/shiningforce3/data/disk2/mpd";
        String outPath = System.getProperty("user.home")+"/project/games/shiningforce3/data/maps2/";


        List<String> files = Files.list(Paths.get(basePath))
                .filter(Files::isRegularFile)
                .map(Path::getFileName)
                .map((Path::toString))
                .map(name -> name.replace(".mpd", ""))
                .filter(name -> !name.equals("ship2"))
                .sorted()
                .collect(Collectors.toList());
//        files = Arrays.asList(
//                "s_rm01"
              //"nasu00", "a_rm01", "s_rm01", "sara06", "s_rm02", "s_rm03", "striin", "stri"
//        );

        Statistics statistics = new Statistics();
        for (String file : files) {
            try {
                Path path = Paths.get(basePath, file + ".mpd");

                Block mpdFile = new MpdReadWork().readFile(path, Paths.get(outPath, file));
                fillStatistics(statistics, file, mpdFile);
                decodeScrollPane(outPath, file, mpdFile);
                Files.writeString(Paths.get(outPath, file, file + ".json"), Utils.toPrettyFormat(mpdFile.toString()));
                System.out.println("done " + file);
            } catch (Exception e) {
                e.printStackTrace();
                System.out.printf("cannot decode file %s: %s%n", file, e.getMessage());
            }
        }
        System.out.println(statistics.toMarkdown());
    }

    private static void fillStatistics(Statistics statistics, String file, Block mpdFile) {
//        statistics.addField(file, "header_short[0]", mpdFile.getObject("header", "short[0]"));
//        statistics.addField(file, "header_short[1]", mpdFile.getObject("header", "short[1]"));
//        statistics.addField(file, "header_value_01", mpdFile.getObject("header", "value_01"));
//        statistics.addField(file, "header_value_02", mpdFile.getObject("header", "value_02"));
//        statistics.addField(file, "unknown_angle",
//                mpdFile.getObject("header", "unknown_angle")+" "+new Angle(
//                        mpdFile.<HexValue>getObject("header", "unknown_angle").getValue()
//                )
//        );
//
//        statistics.addField(file, "header_value_05",
//                new HexValue(mpdFile.<HexValue>getObject("header", "value_05").getValue())
//        );
//        statistics.addField(file, "header_value_06",
//                new Fixed(mpdFile.<HexValue>getObject("header", "value_06").getValue())
//        );
//        statistics.addField(file, "header_value_05a",
//                (short) ((mpdFile.<HexValue>getObject("header", "value_05").getValue() >> 16) & 0xffff)
//        );
//        statistics.addField(file, "header_value_05b",
//                (short) ((mpdFile.<HexValue>getObject("header", "value_05").getValue()) & 0xffff)
//        );
//        statistics.addField(file, "header_value_06a",
//                (short) ((mpdFile.<HexValue>getObject("header", "value_06").getValue() >> 16) & 0xffff)
//        );
//        statistics.addField(file, "header_value_06b",
//                (short) ((mpdFile.<HexValue>getObject("header", "value_06").getValue()) & 0xffff)
//        );
//        statistics.addField(file, "gouraud_tables",
//                ((mpdFile.<MapObjects>getObject("map_objects")).getGouraudTables())
//        );
//        statistics.addField(file, "unknown field upper 16 bits",
//                ((mpdFile.<MapObjects>getObject("map_objects")).getUnknown1Stuff())
//        );
//        statistics.addField(file, "unknown field lower 16 bits",
//                ((mpdFile.<MapObjects>getObject("map_objects")).getUnknown2Stuff())
//        );
    }

    private static void decodeScrollPane(String outPath, String file, Block mpdFile) {
        try {
            // see structure ROTSCROLL
            // KAST Coefficient table start address KAst
            // DKAST Coefficient table vertical direction address increment DKAst
            // DKA Coefficient table vertical direction address increment DKA

            // read scroll screen cells
            List<List<Integer>> palettes = mpdFile.getBlock("header").getObject("palettes");
            parseScrollPlanes(outPath, file, mpdFile, palettes.get(0), Arrays.asList(0, 1), Arrays.asList(2, 5));
            parseScrollPlanes(outPath, file, mpdFile, palettes.get(1), Arrays.asList(3, 4), Collections.singletonList(5));
        } catch (Exception e) {
            System.out.println("cannot decode scroll page: "+e);
        }
    }

    private static void parseScrollPlanes(String outPath, String file, Block mpdFile, List<Integer> palette, List<Integer> cellChunks, List<Integer> pageChunks) throws IOException {
        List<BufferedImage> images = new ArrayList<>();
        for (int chunkNo : cellChunks) {
            images.addAll(readScrollCells(palette, outPath, file, chunkNo));
        }

        List<BufferedImage> pages = new ArrayList<>();
        for (int chunkNo : pageChunks) {
            pages.addAll(readPages(outPath, file, chunkNo, images));
        }

        writeScrollCells(outPath, file, images);
        writeScrollPages(outPath, file, pages);
        writeScrollPlanes(mpdFile, outPath, file, pages, 0);
    }

    private static void writeScrollCells(String outPath, String file, List<BufferedImage> images) throws IOException {
        // calculate columns:
        // * each image is exaclty 8x8 pixels
        // * 2 images in each row
        // * max image height: 512 pixels
        int cellsPerRow = 4;
        int imageHeight = 256;
        int imagesPerColumn = (imageHeight / 8) * cellsPerRow;
        int columns = images.size() / imagesPerColumn;
        int width = columns * cellsPerRow * 8;
        BufferedImage cellImage = new BufferedImage(width, imageHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = cellImage.createGraphics();
        for (int i = 0; i < images.size(); i+=cellsPerRow) {
            int column = i / imagesPerColumn;
            int row = (i % imagesPerColumn) / cellsPerRow;
            for (int c = 0; c < cellsPerRow; c++) {
                graphics.drawImage(images.get(i + c), column * (cellsPerRow * 8) + c * 8, row * 8, null);
            }
            if (row == 0) {
                graphics.setColor(Color.MAGENTA.darker().darker().darker());
                graphics.drawLine((column-1) * (cellsPerRow*8), 0, (column-1) * (cellsPerRow*8), imageHeight);
                graphics.setColor(Color.WHITE);
                graphics.drawString(String.format("%03x", i), column * (cellsPerRow * 8), (row+1) * 8);
            }
        }
        graphics.dispose();
        ImageIO.write(cellImage, "png", Paths.get(outPath, file, file +".mpd.cells.png").toFile());
    }

    private static void writeScrollPlanes(Block mpdFile, String outPath, String file, List<BufferedImage> pages, int plane) throws IOException {
        Block scrollPlanesBlock = mpdFile.createBlock("scroll_planes");
        List<String> scrollPlanes = scrollPlanesBlock.addProperty("values", new ArrayList<>());
        BufferedImage planeImage = new BufferedImage(64 * 8 * 4, 64 * 8 * 4, BufferedImage.TYPE_INT_RGB);
        Graphics2D planeGraphics = planeImage.createGraphics();
        planeGraphics.setColor(Color.MAGENTA);
        for (int i = 0; i < pages.size(); i++) {
            int x = i % 4;
            int y = i / 4;
            planeGraphics.drawImage(pages.get(i), x * 64 * 8, y * 64 * 8, null);
            planeGraphics.drawRect(x * 64 * 8, y * 64 * 8, 64 * 8, 64 * 8);
        }
        planeGraphics.dispose();
        Path scrollPlaneFileName = Paths.get(outPath, file, file + ".mpd.other2.scrollpane" + plane + ".png");
        ImageIO.write(planeImage, "png", scrollPlaneFileName.toFile());
        scrollPlanes.add("\""+scrollPlaneFileName.toAbsolutePath().toString()+"\"");
    }

    /** reads the pattern name data from the file and creates (with the cells) the page images. */
    private static List<BufferedImage> readPages(String outPath, String file, int num, List<BufferedImage> cells) throws IOException {
        Path path = Paths.get(outPath, file, file + ".mpd.other" + num + ".decompressed");
        if (!Files.exists(path)) {
            return Collections.emptyList();
        }
        byte[] patternData = Files.readAllBytes(path);
        try (ImageInputStream patternStream = new ByteArrayImageInputStream(patternData)) {
            patternStream.setByteOrder(ByteOrder.BIG_ENDIAN);
            // cells for pages (aka pattern name data). the page pulls the next 64x64 elements from the list and creates the image
            List<BufferedImage> pageData = new ArrayList<>();
            for (int i = 0; i < patternData.length / 2; i++) {
                int pattern = patternStream.readUnsignedShort();
                int character = (pattern >> 1) & 0x7ff; // 12 bits character number
                pageData.add(cells.get(character));
            }

            List<BufferedImage> pages = new ArrayList<>();
            Iterator<BufferedImage> pageDataIterator = pageData.listIterator();
            for (int p = 0;  p < (pageData.size() / (64*64)); p++) {
                BufferedImage pageImage = new BufferedImage(64*8, 64*8, BufferedImage.TYPE_INT_RGB);
                Graphics2D pageGraphics = pageImage.createGraphics();
                pageGraphics.setColor(Color.CYAN.darker().darker().darker());
                for (int i = 0; i < 64*64; i++) {
                    int x = i % 64;
                    int y = i / 64;
                    pageGraphics.drawImage(pageDataIterator.next(), x*8, y*8, null);
                }
                pageGraphics.dispose();
                pages.add(pageImage);
            }
            return pages;
        }
    }

    private static void writeScrollPages(String outPath, String file, List<BufferedImage> pageImages) throws IOException {
        for (int page = 0; page < pageImages.size(); page++) {
            BufferedImage pageImage = pageImages.get(page);
            ImageIO.write(pageImage, "png", Paths.get(outPath, file, file +".mpd.other2.scroll"+page+"a.png").toFile());
        }
    }

    private static List<BufferedImage> readScrollCells(List<Integer> palette, String outPath, String file, int chunkNo) throws IOException {
        Path path = Paths.get(outPath, file, file + ".mpd.other" + chunkNo + ".decompressed");
        List<BufferedImage> images = new ArrayList<>();
        byte[] data = Files.readAllBytes(path);
        try (ImageInputStream stream = new ByteArrayImageInputStream(data)) {
            stream.setByteOrder(ByteOrder.BIG_ENDIAN);
            for (int cel = 0; cel < data.length / (8 * 8); cel++) {
                // cells are always 8x8 dots (aka pixels)
                BufferedImage image = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);
                for (int y = 0; y < 8; y++) {
                    for (int x = 0; x < 8; x++) {
                        int col = stream.read();
                        image.setRGB(x, y, palette.get(col));
                    }
                }
                images.add(image);
            }
        }
        return images;
    }

    protected Block readHeader(HighlightGroups highlightGroups, Block file, ImageInputStream stream) throws IOException {
        Block header = file.createBlock("header", 0, 0x4);
        // note: mpd files will be placed ~ 0x00290000
        // @see "https://forums.shiningforcecentral.com/viewtopic.php?f=10&t=45857&sid=777c738b5498cb68c3e2f0a5e8c6980b#p804838"
        stream.seek(0);
        highlightGroups.addGroup("file_header", 0x009933);
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
        header.addProperty("offset[2]", new HexValue(readPointer(stream, highlightGroups, "header_offset2")));
        header.addProperty("short[1]", new HexValue(stream.readUnsignedShort()));
        header.addProperty("zero[1]", new HexValue(stream.readUnsignedShort()));

        // some sublists which seems to relate to texture indices or groups.
        // note: looks like missing (moveable/interactible) objects will won't have stuff at offset_4 too
        header.addProperty("offset_4", new HexValue(readPointer(stream, highlightGroups, "header_offset4")));
        // list of texture animation groups
        header.addProperty("offset_texture_animations", new HexValue(readPointer(stream, highlightGroups, "texture_animations")));

        header.addProperty("offset_6", new HexValue(readPointer(stream, highlightGroups, "file_header")));
        header.addProperty("offset_7", new HexValue(readPointer(stream, highlightGroups, "file_header")));
        // at most 3 moveable or interactable objects (chests, barrel)
        header.addProperty("offset_object_1", new HexValue(readPointer(stream, highlightGroups, "header_objects")));
        header.addProperty("offset_object_2", new HexValue(readPointer(stream, highlightGroups, "header_objects")));
        header.addProperty("offset_object_3", new HexValue(readPointer(stream, highlightGroups, "header_objects")));

        header.addProperty("value_01", new HexValue(stream.readInt()));
        header.addProperty("value_02", new HexValue(stream.readInt()));
        // looks like these are animated texture indices.
        header.addProperty("texture_anim_alternative_offset", new HexValue(readPointer(stream, highlightGroups, "header_offset11")));
        header.addProperty("offset_palette1", new HexValue(readPointer(stream, highlightGroups, "palette")));
        header.addProperty("offset_palette2", new HexValue(readPointer(stream, highlightGroups, "palette")));
        header.addProperty("scroll_plane_x", stream.readShort());
        header.addProperty("scroll_plane_y", stream.readShort());
        header.addProperty("scroll_plane_z", stream.readShort());
        header.addProperty("unknown_angle", new HexValue(stream.readUnsignedShort()));
        header.addProperty("value_05", new HexValue(stream.readInt()));
        // ie "0x1860000", => offset 60000 in Registers?
        header.addProperty("value_06", new HexValue(stream.readInt()));
        // might also be 2 "rows" of 4 shorts each
        header.addProperty("offset_14", new HexValue(readPointer(stream, highlightGroups, "file_header")));

        int streamPosition = (int) stream.getStreamPosition();
        header.addProperty("current_file_offset1", new HexValue(streamPosition));
        header.addProperty("current_relative_file_offset1", new HexValue(streamPosition - subHeader2));

        highlightGroups.addRange("file_header", subHeader2, streamPosition-subHeader2);

        Block meshBlock = header.createBlock("objects", 0, 0);

        readTextureAnimationDescription(highlightGroups, stream, header);

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
        readStuffAtOffset6(highlightGroups, stream, header);
        readStuffAtOffset7(highlightGroups, stream, header);
        readTextureAnimationAlternatives(highlightGroups, stream, header);
        readStuffAtOffset14(highlightGroups, stream, header);


        List<List<Integer>> palettes = new ArrayList<>();
        palettes.add(readPalette(highlightGroups, stream, header.getInt("offset_palette1"), header.getInt("offset_palette2")));
        palettes.add(readPalette(highlightGroups, stream, header.getInt("offset_palette2"), header.getInt("offset_sub_header2")));
        header.addProperty("palettes", palettes);
        return header;
    }

    private void readStuffAtOffset7(HighlightGroups highlightGroups, ImageInputStream stream, Block header) throws IOException {
        int offset = header.getInt("offset_7");
        Block stuff = header.createBlock("stuff_at_offset7", offset, 0);

        stream.seek(offset);
        List<HexValue> values = stuff.addProperty("values", new ArrayList<HexValue>());
        while (true) {
            int value = stream.readUnsignedByte();
            if (value == 0xff) {
                break;
            }
            values.add(new HexValue(value));
        }
        stuff.addProperty("size", new HexValue(values.size()));
        highlightGroups.addRange("header_offset7", offset, (int) (stream.getStreamPosition() - offset));
    }

    private void readStuffAtOffset6(HighlightGroups highlightGroups, ImageInputStream stream, Block header) throws IOException {
        int offset = header.getInt("offset_6");
        Block stuff = header.createBlock("stuff_at_offset6", offset, 0);

        stream.seek(offset);
        List<HexValue> values = stuff.addProperty("values", new ArrayList<HexValue>());
        while (true) {
            int value = stream.readUnsignedShort();
            if (value == 0xffff) {
                break;
            }
            values.add(new HexValue(value));
        }
        highlightGroups.addRange("header_offset6", offset, (int) (stream.getStreamPosition() - offset));
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
        List<HexValue> values = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            values.add(new HexValue(stream.readShort()));
        }
        stuff.addProperty("values", values);
        highlightGroups.addRange("header_offset14", offset, 0x10);
    }

    private void readTextureAnimationAlternatives(HighlightGroups highlightGroups, ImageInputStream stream, Block header) throws IOException {
        int offset = header.getInt("texture_anim_alternative_offset");
        Block stuff = header.createBlock("texture_animation_alternatives", offset, 0);
        stream.seek(offset);
        List<HexValue> values = new ArrayList<>();
        // only read some values when the next area is not the same as this area (so this areas size is zero)
        while (true) {
            int value = stream.readUnsignedShort();
            if (value == 0xffff || values.size() > 256) {
                break;
            }
            values.add(new HexValue(value));
        }
        stuff.addProperty("values", values);
        highlightGroups.addRange("texture_animation_alternatives", offset, (values.size()+1) * 2);
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
        List<HexValue> values = new ArrayList<>();
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
