package com.sf3.gamedata.sprites;

import com.sf3.gamedata.utils.HexValue;
import lombok.Data;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;

@Data
public class SpriteSheetHeader {
    private final int width;
    private final int height;
    private final int unknown1;
    private final int unknown2;
    private final int unknown3;
    private final int unknown4;
    private final int unknown5;
    private final int spritesOffset;
    private final int animationsOffset;

    public SpriteSheetHeader(ImageInputStream stream) throws IOException {
        this.width = stream.readUnsignedShort();
        this.height = stream.readUnsignedShort();
        this.unknown1 = stream.readUnsignedShort();
        this.unknown2 = stream.readUnsignedShort();
        this.unknown3 = stream.readUnsignedShort();
        this.unknown4 = stream.readUnsignedShort();
        this.unknown5 = stream.readUnsignedShort();
        this.spritesOffset = stream.readInt();
        this.animationsOffset = stream.readInt();
    }

    @Override
    public String toString() {
        return "{" +
                "width:" + width +
                ", height:" + height +
                ", unknown1:\"" + new HexValue(unknown1) + "\"" +
                ", unknown2:\"" + new HexValue(unknown2) + "\"" +
                ", unknown3:" + unknown3 +
                ", unknown4:" + unknown4 +
                ", unknown5:" + unknown5 +
                ", spritesOffset: \"" + new HexValue(spritesOffset) + "\"" +
                ", animationsOffsets:\"" + new HexValue(animationsOffset) + "\"" +
                '}';
    }
}
