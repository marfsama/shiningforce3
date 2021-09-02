package com.sf3.gamedata.mpd;

import com.sf3.gamedata.utils.HexValue;
import lombok.Getter;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;

/** Pointer to some memory location. This pointer will calculate the relative popsition in the source file. */
@Getter
public class Pointer {
    private final HexValue value;
    private final HexValue relativeOffset;

    public Pointer(ImageInputStream stream, int relativeOffset) throws IOException {
        this.value = new HexValue(stream.readInt());
        this.relativeOffset = new HexValue(value.getValue() - relativeOffset);
    }

    @Override
    public String toString() {
        return "\"" + relativeOffset + " (raw: " + value + ")\"";
    }
}
