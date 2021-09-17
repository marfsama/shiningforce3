package com.sf3.gamedata.mpd;

import com.sf3.util.Sf3Util;

import javax.imageio.stream.ImageInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BinaryOperator;

public class Surface2 {
    private final int width = 64;
    private final int height = 64;

    /** List of 64x64 Integers, each byte corresponding to the height of the tile. */
    private final List<Integer> heightMap = new ArrayList<>();
    /** List of 64x64 unknown Integers (16 bit). Might be tile center height for ramp movement. */
    private final List<Integer> unknown1 = new ArrayList<>();
    /** List of 64x64 unknown Integers (8 bit). */
    private final List<Integer> trigger = new ArrayList<>();

    public Surface2(ImageInputStream stream) {
        for (int i = 0; i < 64 * 64; i++) {
            heightMap.add(Sf3Util.readInt(stream));
        }
        for (int i = 0; i < 64 * 64; i++) {
            unknown1.add(Sf3Util.readUnsignedShort(stream));
        }
        for (int i = 0; i < 64 * 64; i++) {
            trigger.add(Sf3Util.readUnsignedByte(stream));
        }
    }

    public Integer getHeightMapValue(int x, int y) {
        return heightMap.get(y * width + x);
    }

    public Integer getUnknown1(int x, int y) {
        return unknown1.get(y * width + x);
    }

    public Integer getTrigger(int x, int y) {
        return trigger.get(y * width + x);
    }

    private List<String> listToString(BinaryOperator<Integer> valueFunction) {
        List<String> heights = new ArrayList<>();
        for (int y = 0; y < height; y++) {
            List<String> heightLine = new ArrayList<>();
            for (int x = 0; x < width; x++) {
                heightLine.add((valueFunction.apply(x,y)).toString());
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
                "\"trigger\": " +"["+String.join(", ", listToString(this::getTrigger))+"]" +
                "}";
    }
}
