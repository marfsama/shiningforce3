package com.sf3.gamedata.mpd;

import com.sf3.util.Sf3Util;

import javax.imageio.stream.ImageInputStream;

/** A tile represents the 4 corners of a polygon. */
public class PolygonTile extends Tile<Integer>{

    public PolygonTile(ImageInputStream stream) {
        super(2, 2, Sf3Util::readUnsignedByte, stream);
    }

    public int xyToIndex(int x, int y) {
        if (y == 1) {
            return 1-x;
        }
        return 2 + x;
    }
}
