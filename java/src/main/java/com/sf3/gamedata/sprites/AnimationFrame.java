package com.sf3.gamedata.sprites;

import lombok.Data;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;

@Data
class AnimationFrame {
    private final int spriteIndex;
    // speed?
    private final int unknown;

    public AnimationFrame(ImageInputStream stream) throws IOException {
        this.spriteIndex = stream.readUnsignedShort();
        this.unknown = stream.readUnsignedShort();
    }

    @Override
    public String toString() {
        return "{" +
                "spriteIndex: " + spriteIndex +
                ", unknown:" + unknown +
                '}';
    }
}
