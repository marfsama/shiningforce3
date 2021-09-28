package com.sf3.gamedata.mpd;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sf3.binaryview.HighlightGroups;
import com.sf3.gamedata.sgl.*;
import com.sf3.gamedata.utils.HexValue;
import lombok.Data;
import lombok.Getter;
import lombok.SneakyThrows;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Getter
public class MapObjects {
    @JsonIgnore
    private final int offset;
    @JsonIgnore
    private final Pointer offsetLineSegments;
    @JsonIgnore
    private final Pointer offsetCollisionPages;

    private final int numObjects;
    @JsonIgnore
    private final int headerZero;
//    @JsonIgnore
    private final List<ModelHead> models;
    /** List of collision line segments. */
    private final LineSegments lineSegments;
    /** Map of 16x16 pages (row major) and a list of line segments which are (partly or complete) in this page. */
    private final Map<Integer, List<Integer>> collisionPages;

    public MapObjects(ImageInputStream stream, int relativeOffset, HighlightGroups highlightGroups) throws IOException {
        this.offset = (int) stream.getStreamPosition();

        this.offsetLineSegments = new Pointer(stream, relativeOffset);
        this.offsetCollisionPages = new Pointer(stream, relativeOffset);

        this.numObjects = stream.readUnsignedShort();
        this.headerZero = stream.readUnsignedShort();

        highlightGroups.addPointer("map_objects_header", offset, offsetLineSegments.getRelativeOffset().getValue());
        highlightGroups.addPointer("map_objects_header", offset+4, offsetCollisionPages.getRelativeOffset().getValue());
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

        this.lineSegments = readCollisionLineSegments(highlightGroups, stream, relativeOffset, offsetLineSegments, offsetCollisionPages);
        this.collisionPages = readCollisionPages(highlightGroups, stream, relativeOffset, offsetCollisionPages);
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

    @Data
    private static class Stuff2 {
        public HashMap<Integer, List<Integer>> collisionPages;
    }

    private Map<Integer, List<Integer>> readCollisionPages(HighlightGroups highlightGroups, ImageInputStream stream, int relativeOffset, Pointer offsetCollisionPages) throws IOException {
        if (offsetCollisionPages.getValue().getValue() == 0) {
            return null;
        }
        // read stuff at offset 2
        stream.seek(offsetCollisionPages.getRelativeOffset().getValue());
        List<HexValue> offsets = new ArrayList<HexValue>();
        for (int i = 0; i < 256; i++) {
            int offsetValue = stream.readInt();
            offsets.add(new HexValue(offsetValue - relativeOffset));
        }
        // these values index into the list at stuff_at_offset_1.values_2
        Map<Integer, List<Integer>> collisionPages = new LinkedHashMap<>();
        int offsetIndex = 0;
        for (HexValue singleOffset : offsets) {
            stream.seek(singleOffset.getValue());
            List<Integer> values = new ArrayList<>();
            while (true) {
                int value = stream.readUnsignedShort();
                if (value == 0xffff) {
                    break;
                }
                values.add(value);
            }
            collisionPages.put(offsetIndex, values);
            offsetIndex++;
        }
        highlightGroups.addRange("map_collision_pages", offsetCollisionPages.getRelativeOffset().getValue(), (int) (stream.getStreamPosition() - offsetCollisionPages.getRelativeOffset().getValue()));
        return collisionPages;
    }

    @Data
    public static class LineSegment {
        private final int left;
        private final int right;
        private final Point normal;
    }

    @Data
    public static class LineSegments {
        public List<Point> points;
        public List<LineSegment> lineSegments;
    }

    private LineSegments readCollisionLineSegments(HighlightGroups highlightGroups, ImageInputStream stream, int relativeOffset, Pointer offsetLineSegments, Pointer pointer2) throws IOException {
        if (offsetLineSegments.getValue().getValue() == 0) {
            return null;
        }
        LineSegments stuff = new LineSegments();
        stream.seek(offsetLineSegments.getRelativeOffset().getValue());
        int length = pointer2.getValue().getValue() - offsetLineSegments.getValue().getValue();
        Pointer offset1 = new Pointer(stream, relativeOffset);
        Pointer offset2 = new Pointer(stream, relativeOffset);

        highlightGroups.addPointer("map_objects_header1a", offsetLineSegments.getRelativeOffset().getValue(), offset1.getRelativeOffset().getValue());
        highlightGroups.addPointer("map_objects_header1b", offsetLineSegments.getRelativeOffset().getValue()+4, offset2.getRelativeOffset().getValue());
        stream.seek(offset1.getRelativeOffset().getValue());
        int size1 = (offset2.getRelativeOffset().getValue() - offset1.getRelativeOffset().getValue()) / 4;
        List<Point> values1 = new ArrayList<>();
        // note: list seems to be terminated by 0xff
        for (int i = 0; i < size1; i++) {
            Fixed x = new Fixed(stream.readShort() << (8+3));
            Fixed y = new Fixed(stream.readShort() << (8+3));
            values1.add(new Point(x,y,new Fixed(0)));
        }
        stuff.points = values1;
        highlightGroups.addRange("mo_points", offset1.getRelativeOffset().getValue(), (int) (stream.getStreamPosition() - offset1.getRelativeOffset().getValue()));

        stream.seek(offset2.getRelativeOffset().getValue());
        int size2 = (pointer2.getRelativeOffset().getValue() - offset2.getRelativeOffset().getValue()) / 8;
        List<LineSegment> values2 = new ArrayList<>();

        for (int i = 0; i < size2; i++) {
            int leftPointIndex = stream.readUnsignedShort();
            int rightPointIndex = stream.readUnsignedShort();
            Fixed normalX = new Fixed(stream.readUnsignedShort());
            Fixed normalY = new Fixed(stream.readUnsignedShort());
            values2.add(new LineSegment(leftPointIndex, rightPointIndex, new Point(normalX, normalY, Fixed.ZERO)));
        }
        stuff.lineSegments = values2;
        highlightGroups.addRange("mo_linesegments", offset2.getRelativeOffset().getValue(), (int) (stream.getStreamPosition() - offset2.getRelativeOffset().getValue()));
        return stuff;
    }


    @SneakyThrows
    @Override
    public String toString() {
        ObjectMapper mapper = new ObjectMapper();

        return mapper.writeValueAsString(this);
    }
}
