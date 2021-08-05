package com.sf3.gamedata.mpd;

import com.sf3.gamedata.utils.HexValue;

import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class Texture {
    private BufferedImage image;
    /**
     * texture index
     */
    private int textureId;
    private final int width;
    private final int height;
    private final int offset;
    private int textureImageIndex;

    public Texture(int textureId, int width, int height, int offset) {
        this.textureId = textureId;
        this.width = width;
        this.height = height;
        this.offset = offset;
    }

    public Texture(int textureId, ImageInputStream stream) throws IOException {
        this.textureId = textureId;
        this.width = stream.readUnsignedByte();
        this.height = stream.readUnsignedByte();
        this.offset = stream.readUnsignedShort();
    }

    public BufferedImage getImage() {
        return image;
    }

    public int getTextureImageIndex() {
        return textureImageIndex;
    }

    public void setTextureImageIndex(int textureImageIndex) {
        this.textureImageIndex = textureImageIndex;
    }

    public void setImage(BufferedImage image) {
        this.image = image;
    }

    public int getTextureId() {
        return textureId;
    }

    public void setTextureId(int textureId) {
        this.textureId = textureId;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getOffset() {
        return offset;
    }

    @Override
    public String toString() {
        return "{" +
                "\"textureId\":" + new HexValue(textureId) +
                ", \"width\":" + width +
                ", \"height\":" + height +
                ", \"offset\":" + new HexValue(offset) +
                ", textureImageIndex: "+textureImageIndex+
                '}';
    }
}
