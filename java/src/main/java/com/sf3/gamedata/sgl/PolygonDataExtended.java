package com.sf3.gamedata.sgl;

import com.sf3.gamedata.utils.HexValue;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;

/**
 * Polygon Data Structure extended by another pointer.
 *
 * @see "SGL Strucure Reference, page 3, XPDATA"
 */
public class PolygonDataExtended extends PolygonData {
    public static final int SIZE = PolygonData.SIZE + 4;

    private final int vertexNormalsOffset;

    public PolygonDataExtended(ImageInputStream stream, String name) throws IOException {
        super(stream, name, SIZE);
        this.vertexNormalsOffset = stream.readInt();
        // 6 bytes for each point. another vector?
        addProperty("vertexNormalsOffset", new HexValue(vertexNormalsOffset));
    }



}
