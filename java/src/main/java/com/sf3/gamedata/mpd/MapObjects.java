package com.sf3.gamedata.mpd;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sf3.binaryview.HighlightGroups;
import com.sf3.gamedata.sgl.Point;
import com.sf3.gamedata.sgl.Polygon;
import com.sf3.gamedata.sgl.PolygonAttribute;
import com.sf3.gamedata.sgl.PolygonData;
import lombok.Getter;
import lombok.SneakyThrows;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

    @SneakyThrows
    @Override
    public String toString() {
        ObjectMapper mapper = new ObjectMapper();

        return mapper.writeValueAsString(this);
    }
}
