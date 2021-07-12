package com.sf3.gamedata.sgl;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;

/**
 * Polygon Attributes.
 * @see "SGL Structure Reference, page 1"
 * @see "SGL Developers Manual, Programmers Tutorial, Chapter 7"
 */
public class PolygonAttribute {
    /** Structure size in bytes. */
    public static final int SIZE = 12;
    public static final int DRAW_MODE_TEXTURED = 2;
    public static final int DRAW_MODE_POLY = 4;

    /** Front/Back culling. */
    private final int flag;
    /** Representative Point for z-sort. */
    private final int sort;
    /** Texture no. */
    private final int texno;
    /** Atributes and Modes. @see "SGL Programmers Manual, page 7-17".*/
    private final int attributes;
    /** RGB Color or Color Bank, depending on Mode. */
    private final int colno;
    /** Gouraud Shading table. */
    private final int gouraudTable;
    /**
     * Texture flipping and Polygon/Line setting.
     * <pre>
     * Bits 0-3
     *   - 0 - nothing
     *   - 1 draw sprite
     *   - 2 draw texture
     *   - 4 draw polygon
     *   - 5 draw polyline
     *   - 6 draw line
     *   - 8 user clip
     *   - 9 system clip (not used)
     *   - 10 base position
     * Bit 4 - Flip X
     * Bit 5 - Flip Y
     * Bit 6 - unknown
     * Bit 7 - unknown
     * </pre>
     * */
    private final int dir;

    public PolygonAttribute(ImageInputStream stream) throws IOException {
        this.flag = stream.readUnsignedByte();
        this.sort = stream.readUnsignedByte();
        this.texno = stream.readUnsignedShort();
        this.attributes = stream.readUnsignedShort();
        this.colno = stream.readUnsignedShort();
        this.gouraudTable = stream.readUnsignedShort();
        this.dir = stream.readUnsignedShort();
    }

    public int getFlag() {
        return flag;
    }

    public int getSort() {
        return sort;
    }

    public int getTexno() {
        return texno;
    }

    public int getAttributes() {
        return attributes;
    }

    public int getColno() {
        return colno;
    }

    public int getGouraudTable() {
        return gouraudTable;
    }

    public int getDir() {
        return dir;
    }

    @Override
    public String toString() {
        return "{" +
                "'plane':" + flag +
                ", 'sort': 0x" + Integer.toHexString(sort) +
                ", 'texno': " + texno +
                ", 'atrb': 0x" + Integer.toHexString(attributes) +
                ", 'colno':0x" + Integer.toHexString(colno) +
                ", 'gstb': 0x" + Integer.toHexString(gouraudTable) +
                ", 'dir':0x" +Integer.toHexString(dir) +
                '}';
    }
}
