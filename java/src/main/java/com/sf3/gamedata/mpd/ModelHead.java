package com.sf3.gamedata.mpd;

import com.sf3.gamedata.utils.Block;
import com.sf3.gamedata.utils.HexValue;
import com.sf3.gamedata.sgl.Angle;
import com.sf3.gamedata.sgl.Fixed;
import com.sf3.gamedata.sgl.Point;
import com.sf3.gamedata.sgl.PolygonData;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Model Head consisting of up to 8 variants (mostly skins) of a model. */
public class ModelHead extends Block {
    private static final int SIZE = 60;
    private final List<Pointer> pointers;
    private final int relativeOffset;
    private List<PolygonData> polygonData;

    public ModelHead(ImageInputStream stream, String name, int relativeOffset) throws IOException {
        super(name, (int) stream.getStreamPosition(), SIZE);
        this.relativeOffset = relativeOffset;
        addProperty("offset", new HexValue(getStart()));

        // these are 8 pointers to a mesh structure (PDATA, Polygon model data with different skins)
        this.pointers = new ArrayList<>();
        List<HexValue> offsets = new ArrayList<>();
        List<HexValue> rawOffsets = new ArrayList<>();
        for (int a = 0; a < 8; a++) {
            Pointer pointer = new Pointer(stream, relativeOffset);
            if (pointer.getValue() > 0) {
                offsets.add(new HexValue(pointer.getRelativeOffset()));
                rawOffsets.add(new HexValue(pointer.getValue()));
                pointers.add(pointer);
            }
        }
        addProperty("polydata_rawpointers", rawOffsets.toString());
        addProperty("polydata_pointers", offsets.toString());

        addProperty("position", new Point(
                new Fixed(stream.readShort() << 16),
                new Fixed(stream.readShort() << 16),
                new Fixed(stream.readShort() << 16)
                ));

        addProperty("rotation", Arrays.asList(
                new Angle(stream.readUnsignedShort()).getRadians(),
                new Angle(stream.readUnsignedShort()).getRadians(),
                new Angle(stream.readUnsignedShort()).getRadians()
        ));

        addProperty("scale", new Point(stream));
        addProperty("padding", stream.readInt());
    }

    public List<PolygonData> getPolygonData() {
        return polygonData;
    }

    public void readData(ImageInputStream stream) throws IOException {
        this.polygonData = new ArrayList<PolygonData>();
        for (int i = 0; i < pointers.size(); i++) {
            Pointer pointer = pointers.get(i);
            if (pointer.getRelativeOffset() < 0) {
                throw new IOException("The relative pointer of the mesh is negative. Maybe the raw pointer is placed in the range 0x60a0000 instead of 0x0292100. Raw pointer is " + new HexValue(pointer.getValue()));
            }
            stream.seek(pointer.getRelativeOffset());
            PolygonData polygonData = new PolygonData(stream, "polygon[" + i + "]");
            polygonData.readDetails(stream, relativeOffset);
            this.polygonData.add(polygonData);
        }
        addProperty("polygonData",polygonData);

    }
}
