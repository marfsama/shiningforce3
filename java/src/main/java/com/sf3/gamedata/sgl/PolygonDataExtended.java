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

    public PolygonDataExtended(ImageInputStream stream) throws IOException {
        super(stream);
        this.vertexNormalsOffset = stream.readInt();
    }



}
