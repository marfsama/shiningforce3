package com.sf3.vdp1.model;

import com.sf3.util.Sf3Util;

import java.util.ArrayList;
import java.util.List;

/**
 * 4kb color RAM.
 *
 * (1) Mode 0: RGB in each of 5 bits for a total of 15 bits, 1024 color settings
 * (2) Mode 1: RGB in each of 5 bits for a total of 15 bits, 2048 color settings
 * (3) Mode 2: RGB in each of 8 bits for a total of 24 bits, 1024 color settings
 *
 * @see "VDP2 User Manual, page 43"
 */
public class ColorRam {
    private final int type;
    private final byte[] content;
    private final List<Integer> colors = new ArrayList<>();

    public ColorRam(byte type, byte[] content) {
        this.type = type;
        this.content = content;
        System.out.println("Color RAM Size: "+content.length);
        for (int i = 0; i < getNumColors(); i++) {
            int col16bit = ((content[i*2] & 0xff) << 8) + (content[i*2+1] & 0xff);
            colors.add(Sf3Util.rgb16ToRgb24(col16bit));
        }
    }

    public int getType() {
        return type;
    }

    public byte[] getContent() {
        return content;
    }

    public int getNumColors() {
        if (type == 1) {
            return 2048;
        }
        return 1024;
    }

    public int getColor(int colorBank, int colorIndex) {
        if (type == 2) {
            // 8 bit/pixel
            int offset = (colorBank + colorIndex * 4);
            int r = content[offset] & 0xff;
            int g = content[offset + 1] & 0xff;
            int b = content[offset + 2] & 0xff;
            return (r << 16) + (g << 8) + b;
        }
        // 5 bits/pixel
        int offset = ((colorBank + colorIndex) * 2) % content.length;
        int word = ((content[offset+1] & 0xff) << 8) + (content[offset] & 0xff);
        return Sf3Util.rgb16ToRgb24(word);
    }
}
