package com.sf3.gamedata.mpd;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sf3.gamedata.sgl.Angle;
import com.sf3.gamedata.sgl.Fixed;
import com.sf3.gamedata.sgl.Point;
import com.sf3.gamedata.sgl.PolygonData;
import com.sf3.gamedata.utils.HexValue;
import lombok.Getter;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Model Head consisting of up to 8 variants of a model. */
@Getter
public class ModelHead {
    private static final int SIZE = 60;
    private final List<Pointer> pointers;
    @JsonIgnore
    private final HexValue relativeOffset;
    @JsonIgnore
    private final HexValue offset;
    @JsonIgnore
    private final String polydataRawPointers;
    @JsonIgnore
    private final String polydataPointers;
    private final Point position;
    private final List<Float> rotation;
    private final Point scale;
    private List<PolygonData> polygonData;

    public ModelHead(ImageInputStream stream, int relativeOffset) throws IOException {
        this.relativeOffset = new HexValue(relativeOffset);
        this.offset = new HexValue((int) stream.getStreamPosition());

        // these are 8 pointers to a mesh structure (PDATA, Polygon model data with different skins)
        this.pointers = new ArrayList<>();
        List<HexValue> offsets = new ArrayList<>();
        List<HexValue> rawOffsets = new ArrayList<>();
        for (int a = 0; a < 8; a++) {
            Pointer pointer = new Pointer(stream, relativeOffset);
            // fix for ship2.mpd:
//            if (pointer.getRelativeOffset() > 0x10_000 || pointer.getRelativeOffset() <= 0) {
//                // if the value doesn't look like pointer: take back the read and stop reading mesh offsets
//                stream.seek(stream.getStreamPosition()-4);
//                break;
//            }
            if (pointer.getValue().getValue() > 0) {
                offsets.add(new HexValue(pointer.getRelativeOffset().getValue()));
                rawOffsets.add(new HexValue(pointer.getValue().getValue()));
                pointers.add(pointer);
            }
        }
        this.polydataRawPointers = rawOffsets.toString();
        this.polydataPointers = offsets.toString();

        this.position = new Point(
                new Fixed(stream.readShort() << 16),
                new Fixed(stream.readShort() << 16),
                new Fixed(stream.readShort() << 16)
                );

        this.rotation = Arrays.asList(
                new Angle(stream.readUnsignedShort()).getRadians(),
                new Angle(stream.readUnsignedShort()).getRadians(),
                new Angle(stream.readUnsignedShort()).getRadians()
        );

        this.scale = new Point(stream);
        // padding
        stream.readInt();
    }

    public List<PolygonData> getPolygonData() {
        return polygonData;
    }

    public void readData(ImageInputStream stream) throws IOException {
        this.polygonData = new ArrayList<>();
        for (Pointer pointer : pointers) {
            if (pointer.getRelativeOffset().getValue() < 0) {
                throw new IOException("The relative pointer of the mesh is negative. Maybe the raw pointer is placed in the range 0x60a0000 instead of 0x0292100. Raw pointer is " + new HexValue(pointer.getValue().getValue()));
            }
            stream.seek(pointer.getRelativeOffset().getValue());
            PolygonData pdata = new PolygonData(stream);
            pdata.readDetails(stream, relativeOffset.getValue());
            this.polygonData.add(pdata);
        }

    }
}
