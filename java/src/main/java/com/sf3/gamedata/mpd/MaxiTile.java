package com.sf3.gamedata.mpd;

import com.sf3.util.StringUtil;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/** A 16x16 Tile of MiniTiles.
 * @param <T> type of each single entry.
 */
public class MaxiTile<T> extends Tile<T>{
    public MaxiTile(int width, int height, List<T> tiles) {
        super(width, height, tiles);
    }

    public MaxiTile(int width, int height, Function<ImageInputStream, T> tileFunction, ImageInputStream stream) {
        super(width, height, tileFunction, stream);
    }

    public int xyToIndex(int x, int y) {
        return (height - y - 1) * width + x;
    }

}
