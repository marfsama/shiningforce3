package com.sf3.gamedata.sgl;

public class TextureUv {
    private final float x1;
    private final float y1;
    private final float x2;
    private final float y2;

    public TextureUv(float x1, float y1, float x2, float y2) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }

    @Override
    public String toString() {
        return "[ " + x1 + " , " + y1 + " , " + x2 + ", " + y2 + " ]";
    }
}
