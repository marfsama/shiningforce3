package com.sf3.gamedata.mpd;

import com.sf3.gamedata.utils.HexValue;
import com.sf3.util.Sf3Util;

import javax.imageio.stream.ImageInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

public class Surface2 {
    private int width = 64;
    private int height = 64;

    /** List of 64x64 Integers, each byte corresponding to the height of the tile. */
    private List<Integer> heightMap = new ArrayList<>();
    /** List of 64x64 unknown Integers (16 bit). Might be tile center height for ramp movement. */
    private List<Integer> unknown1 = new ArrayList<>();
    /** List of 64x64 unknown Integers (8 bit). */
    private List<Integer> unknown2 = new ArrayList<>();

    public Surface2(ImageInputStream stream) {
        for (int i = 0; i < 64 * 64; i++) {
            heightMap.add(Sf3Util.readInt(stream));
        }
        for (int i = 0; i < 64 * 64; i++) {
            unknown1.add(Sf3Util.readUnsignedShort(stream));
        }
        for (int i = 0; i < 64 * 64; i++) {
            unknown2.add(Sf3Util.readUnsignedByte(stream));
        }
    }

    public Integer getHeightMapValue(int x, int y) {
        return heightMap.get(y * width + x);
    }

    public Integer getUnknown1(int x, int y) {
        return unknown1.get(y * width + x);
    }

    public Integer getUnknown2(int x, int y) {
        return unknown2.get(y * width + x);
    }

    private List<String> listToString(BiFunction<Integer, Integer, Integer> valueFunction) {
        List<String> heights = new ArrayList<>();
        for (int y = 0; y < height; y++) {
            List<Integer> heightLine = new ArrayList<>();
            for (int x = 0; x < width; x++) {
                heightLine.add((valueFunction.apply(x,y)));
            }
            heights.add("\""+heightLine+"\"");
        }
        return heights;
    }

    @Override
    public String toString() {
        return "{" +
                "\"heights\": " +"["+String.join(", ", listToString(this::getHeightMapValue))+"]," +
                "\"unknown1\": " +"["+String.join(", ", listToString(this::getUnknown1))+"]," +
                "\"unknown2\": " +"["+String.join(", ", listToString(this::getUnknown2))+"]" +
                "}";
    }
}
