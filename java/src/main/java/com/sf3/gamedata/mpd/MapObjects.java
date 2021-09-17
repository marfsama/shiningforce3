package com.sf3.gamedata.mpd;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sf3.binaryview.HighlightGroups;
import com.sf3.gamedata.sgl.Point;
import com.sf3.gamedata.sgl.Polygon;
import com.sf3.gamedata.sgl.PolygonAttribute;
import com.sf3.gamedata.sgl.PolygonData;
import com.sf3.gamedata.utils.Block;
import com.sf3.gamedata.utils.HexValue;
import lombok.Getter;
import lombok.SneakyThrows;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public class MapObjects {
    @JsonIgnore
    private final int offset;
    @JsonIgnore
    private final Pointer pointer1;
    @JsonIgnore
    private final Pointer pointer2;

    private final int numObjects;
    @JsonIgnore
    private final int headerZero;
    private final List<ModelHead> models;

    public MapObjects(ImageInputStream stream, int relativeOffset, HighlightGroups highlightGroups) throws IOException {
        this.offset = (int) stream.getStreamPosition();

        this.pointer1 = new Pointer(stream, relativeOffset);
        this.pointer2 = new Pointer(stream, relativeOffset);

        this.numObjects = stream.readUnsignedShort();
        this.headerZero = stream.readUnsignedShort();

        highlightGroups.addPointer("map_objects_header", offset, pointer1.getRelativeOffset().getValue());
        highlightGroups.addPointer("map_objects_header", offset+4, pointer2.getRelativeOffset().getValue());
        highlightGroups.addRange("map_objects_header", offset, 0xc);

        this.models = new ArrayList<>();

        for (int i = 0; i < numObjects; i++) {
            ModelHead modelHead = new ModelHead(stream, relativeOffset);
            models.add(modelHead);
        }
        highlightGroups.addRange("mesh head", offset+12, (int) (stream.getStreamPosition() - (offset+12)));

        // read polygon data structure for each model head
        for (ModelHead head : models) {
            head.readData(stream);

            for (PolygonData data : head.getPolygonData()) {
                highlightGroups.addRange("mesh XPDATA", offset, PolygonData.SIZE);
                highlightGroups.addRange("mesh POINT[]", data.getPointsOffset() - relativeOffset, data.getNumPoints() * Point.SIZE);
                highlightGroups.addRange("mesh POLYGON[]", data.getPolygonOffset() - relativeOffset, data.getNumPolygons() * Polygon.SIZE);
                highlightGroups.addRange("mesh ATTR[]", data.getPolygonAttributesOffset() - relativeOffset, data.getNumPolygons() * PolygonAttribute.SIZE);
            }
        }

//        readStuffAtObjectOffset1(highlightGroups, stream, relativeOffset, block, pointer1, pointer2);
//        readStuffAtObjectOffset2(highlightGroups, stream, relativeOffset, block, pointer2);
    }

    public List<HexValue> getGouraudTables() {
        return models.stream()
                .map(ModelHead::getPolygonData)
                .flatMap(Collection::stream)
                .map(PolygonData::getPolygonAttributes)
                .flatMap(Collection::stream)
                .map(PolygonAttribute::getGouraudTable)
                .sorted()
                .distinct()
                .map(HexValue::new)
                .collect(Collectors.toList());
    }

    public List<HexValue> getUnknown2Stuff() {
        return models.stream()
                .map(ModelHead::getUnknown2)
                .sorted()
                .distinct()
                .map(HexValue::new)
                .collect(Collectors.toList());
    }

    public List<HexValue> getUnknown1Stuff() {
        return models.stream()
                .map(ModelHead::getUnknown1)
                .sorted()
                .distinct()
                .map(HexValue::new)
                .collect(Collectors.toList());
    }

//    private void readStuffAtObjectOffset1(HighlightGroups highlightGroups, ImageInputStream stream, int relativeOffset, Block block, Pointer pointer1, Pointer pointer2) throws IOException {
//        // read stuff at offset 1
//        // maybe this is some kind of binary tree for visibility culling of the objects
//        // see bsp in doom (https://en.wikipedia.org/wiki/Binary_space_partitioning)
//
//        if (pointer1.getValue().getValue() == 0) {
//            return;
//        }
//        stream.seek(pointer1.getRelativeOffset().getValue());
//        int length = pointer2.getValue().getValue() - pointer1.getValue().getValue();
//        Block stuffAtOffset1 = block.createBlock("stuff_at_offset_1", pointer1.getRelativeOffset().getValue(), length);
//        Pointer offset1 = new Pointer(stream, relativeOffset);
//        Pointer offset2 = new Pointer(stream, relativeOffset);
//        stuffAtOffset1.addProperty("offset_1", offset1);
//        stuffAtOffset1.addProperty("offset_2", offset2);
//
//        highlightGroups.addPointer("map_objects_header", pointer1.getRelativeOffset().getValue(), offset1.getRelativeOffset().getValue());
//        highlightGroups.addPointer("map_objects_header", pointer1.getRelativeOffset().getValue()+4, offset2.getRelativeOffset().getValue());
//        stream.seek(offset1.getRelativeOffset().getValue());
//        int size1 = (offset2.getRelativeOffset().getValue() - offset1.getRelativeOffset().getValue()) / 4;
//        List<Object> values1 = new ArrayList<>();
//        // note: list seems to be terminated by 0xff
//        for (int i = 0; i < size1; i++) {
//            values1.add(new HexValue(stream.readInt()));
//        }
//        stuffAtOffset1.addProperty("count_1", values1.size());
//        stuffAtOffset1.addProperty("count_1_hex", new HexValue(values1.size()));
//        stuffAtOffset1.addProperty("values_1", values1);
//
//        stream.seek(offset2.getRelativeOffset().getValue());
//        int size2 = (pointer2.getRelativeOffset().getValue() - offset2.getRelativeOffset().getValue()) / 8;
//        List<Object> values2 = new ArrayList<>();
//
//        for (int i = 0; i < size2; i++) {
//            HexValue value1 = new HexValue(stream.readInt());
//            HexValue value2 = new HexValue(stream.readInt());
//            values2.add("\""+value1+" "+value2+"\"");
//        }
//        stuffAtOffset1.addProperty("count_2", values2.size());
//        stuffAtOffset1.addProperty("count_2_hex", new HexValue(values2.size()));
//        stuffAtOffset1.addProperty("values_2", values2);
//        highlightGroups.addRange("map_objects_stuff1", stuffAtOffset1.getStart(), stuffAtOffset1.getLength());
//    }
//
//    private void readStuffAtObjectOffset2(HighlightGroups highlightGroups, ImageInputStream stream, int relativeOffset, Block block, Pointer pointer2) throws IOException {
//        if (pointer2.getValue().getValue() == 0) {
//            return;
//        }
//        // read stuff at offset 2
//        stream.seek(pointer2.getRelativeOffset().getValue());
//        Block stuffAtOffset2 = block.createBlock("stuff_at_offset_2", pointer2.getRelativeOffset().getValue(), 0);
//        List<Integer> offsets = new ArrayList<>();
//        while (true) {
//            int offsetValue = stream.readInt();
//            if (offsetValue == -1) {
//                break;
//            }
//            offsets.add(offsetValue - relativeOffset);
//        }
//        stuffAtOffset2.addProperty("size", offsets.size());
//        Block offsetsBlock = stuffAtOffset2.createBlock("offsets", 0, 0);
//        int min = 0x7fff;
//        int max = 0;
//        // these values index into the list at stuff_at_offset_1.values_2
//        int offsetIndex = 0;
//        for (int singleOffset : offsets) {
//            stream.seek(singleOffset);
//            List<HexValue> values = new ArrayList<>();
//            while (true) {
//                int value = stream.readUnsignedShort();
//                if (value == 0xffff) {
//                    break;
//                }
//                min = Math.min(min, value);
//                max = Math.max(max, value);
//                values.add(new HexValue(value));
//            }
//            offsetsBlock.addProperty(new HexValue(offsetIndex).toString(), values.toString().replace('"',' '));
//            offsetIndex++;
//        }
//        highlightGroups.addRange("map_objects_stuff2", stuffAtOffset2.getStart(), (int) (stream.getStreamPosition() - stuffAtOffset2.getStart()));
//        stuffAtOffset2.addProperty("min", new HexValue(min));
//        stuffAtOffset2.addProperty("max", new HexValue(max));
//    }



    @SneakyThrows
    @Override
    public String toString() {
        ObjectMapper mapper = new ObjectMapper();

        return mapper.writeValueAsString(this);
    }
}
