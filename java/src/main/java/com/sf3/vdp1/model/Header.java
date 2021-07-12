package com.sf3.vdp1.model;

import com.sf3.util.Utils;

import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Date;

/** Save State header. */
public class Header {

    private final String magic;
    private final Date timestamp;
    private final long version;
    private final int width;
    private final int height;
    private final BufferedImage image;

    public Header(ImageInputStream stream) throws IOException {
        this.magic = Utils.readString(stream,8);
        this.timestamp = new Date(stream.readLong()*1000);
        this.version = stream.readLong();

        this.width = stream.readInt();
        this. height = stream.readInt();

        this.image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int r = stream.read();
                int g = stream.read();
                int b = stream.read();
                int pixel = b + (g << 8) + (r << 16);
                image.setRGB(x,y,pixel);
            }
        }
    }

    public String getMagic() {
        return magic;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public long getVersion() {
        return version;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public BufferedImage getImage() {
        return image;
    }

    @Override
    public String toString() {
        return "Header{" +
                "magic='" + magic + '\'' +
                ", timestamp=" + timestamp +
                ", version=" + version +
                ", width=" + width +
                ", height=" + height +
                '}';
    }
}
