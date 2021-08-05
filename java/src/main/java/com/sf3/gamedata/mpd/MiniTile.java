package com.sf3.gamedata.mpd;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** A simple 4x4 Tile. */
public class MiniTile {
    private final List<Integer> tiles;

    public MiniTile(List<Integer> tiles) {
        this.tiles = tiles;
    }

    public MiniTile(ImageInputStream stream) {
        this.tiles = new ArrayList<>();
        try {
            for (int i = 0; i < 16; i++) {
                this.tiles.add(stream.readUnsignedShort());
            }
        } catch (IOException ioe) {
            throw new IllegalStateException(ioe);
        }
    }

    public List<Integer> getTiles() {
        return tiles;
    }

    public int getWidth() {
        return 4;
    }

    public int getHeight() {
        return 4;
    }

    public int get(int index) {
        return tiles.get(index);
    }

    public int get(int x, int y) {
        return get(xyToIndex(x,y));
    }

    protected int xyToIndex(int x, int y) {
        return (3 - y) * 4 + x;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 4; x++) {
                int value = get(x,y);
                String s = Integer.toHexString(value);
                buf.append(" 0x"+(s.length() == 1 ? "0"+s : s ));
            }
            buf.append(" |\n");
        }
        return buf.toString();
    }
}
