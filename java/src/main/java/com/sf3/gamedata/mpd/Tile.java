package com.sf3.gamedata.mpd;

import com.sf3.util.StringUtil;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public abstract class Tile<T> {
    protected final int width;
    protected final int height;
    private final List<T> tiles;
    private final int streamOffset;

    public Tile(int width, int height, List<T> tiles) {
        this.tiles = tiles;
        this.width = width;
        this.height = height;
        this.streamOffset = 0;
    }

    public Tile(int width, int height, Function<ImageInputStream, T> tileFunction, ImageInputStream stream) {
        this.tiles = new ArrayList<>();
        this.width = width;
        this.height = height;
        this.streamOffset = readSteamOffset(stream);
        for (int i = 0; i < width*height; i++) {
            this.tiles.add(tileFunction.apply(stream));
        }
    }

    public int getStreamOffset() {
        return streamOffset;
    }

    private int readSteamOffset(ImageInputStream stream) {
        try {
            return (int) stream.getStreamPosition();
        } catch (IOException e) {
            return -1;
        }
    }

    public List<T> getTiles() {
        return tiles;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public T get(int index) {
        return tiles.get(index);
    }

    public T get(int x, int y) {
        return get(xyToIndex(x,y));
    }

    public abstract int xyToIndex(int x, int y);

    public String dump() {
        StringBuilder buf = new StringBuilder();
        for (int y = 0; y < height; y++) {
            String block = "";
            for (int x = 0; x < width; x++) {
                T tile = get(x, y);
                block = StringUtil.blockConcat(block, tile.toString());
            }
            buf.append(block).append("\n");
        }
        return buf.toString();
    }

    @Override
    public String toString() {
        return "\"Tile{"+width+"x"+height+" tiles (each 4x4)}\"";
    }
}
