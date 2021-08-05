package com.sf3.gamedata.mpd;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;

/** Pointer to some memory location. This pointer will calculate the relative popsition in the source file. */
public class Pointer {
    private final int value;
    private final int relativeOffset;

    public Pointer(ImageInputStream stream, int relativeOffset) throws IOException {
        this.value = stream.readInt();
        this.relativeOffset = value - relativeOffset;
    }

    public int getValue() {
        return value;
    }

    public int getRelativeOffset() {
        return relativeOffset;
    }

    @Override
    public String toString() {
        return "\"0x" + Integer.toHexString(relativeOffset) + " (raw: 0x" + Integer.toHexString(value) + ")\"";
    }
}
