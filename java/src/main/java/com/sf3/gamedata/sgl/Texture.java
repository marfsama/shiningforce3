package com.sf3.gamedata.sgl;

import com.sf3.gamedata.utils.Block;
import com.sf3.gamedata.utils.HexValue;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;

/**
 * Texture definition. Size: 8 bytes.
 *
 * @see "SGL Strucure Reference, page 10"
 */
public class Texture extends Block {
    /** Structure size in bytes. */
    public static final int SIZE = 8;
    /** Uint16, Horizontal size of texture */
    private final int Hsize;
    /** Uint16, Vertical size of texture */
    private final int Vsize;
    /** Uint16, CG address of texture/8 */
    private final int CGadr;
    /** Uint16, Horizontal size/8, vertical size (for hardware) ((HSIZE/8)<<8)|(V SIZE) */
    private final int HVsize;

    public Texture(ImageInputStream stream, String name) throws IOException {
        super(name, (int) stream.getStreamPosition(), SIZE);
        this.Hsize = stream.readUnsignedShort();
        this.Vsize = stream.readUnsignedShort();
        this.CGadr = stream.readUnsignedShort();
        this.HVsize = stream.readUnsignedShort();

        addProperty("width", (getWidth()));
        addProperty("heigth", (getHeight()));
        addProperty("vramAddress", new HexValue(getVramAddress()));
    }

    public int getHeight() {
        return Vsize;
    }

    public int getWidth() {
        return Hsize;
    }

    public int getVramAddress() {
        return CGadr << 3;
    }

    public int getHsize() {
        return Hsize;
    }

    public int getVsize() {
        return Vsize;
    }

    public int getCGadr() {
        return CGadr;
    }

    public int getHVsize() {
        return HVsize;
    }
}
