package com.sf3.vdp1.model;

/** Texture in VRAM. */
public class Texture {
    private final int width;
    private final int height;
    private final ColorMode colorMode;
    private final int offset;
    private final int colorbank;

    public Texture(int width, int height, ColorMode colorMode, int offset, int colorbank) {
        this.width = width;
        this.height = height;
        this.colorMode = colorMode;
        this.offset = offset;
        this.colorbank = colorbank;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public ColorMode getColorMode() {
        return colorMode;
    }

    public int getOffset() {
        return offset;
    }

    public int getColorbank() {
        return colorbank;
    }

    @Override
    public String toString() {
        return "Texture{" +
                "width=" + width +
                ", height=" + height +
                ", colorMode=" + colorMode +
                ", offset=0x" + Integer.toHexString(offset) +
                ", colorbank=0x" + Integer.toHexString(colorbank) +
                '}';
    }
}
