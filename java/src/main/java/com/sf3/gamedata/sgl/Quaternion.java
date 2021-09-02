package com.sf3.gamedata.sgl;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;

/** Rotation around an axis. Not an official SGL class. */
public class Quaternion {
    private final Fixed x;
    private final Fixed y;
    private final Fixed z;
    private final Fixed w;

    public Quaternion(ImageInputStream stream) throws IOException {
        this(new Fixed(stream), new Fixed(stream), new Fixed(stream), new Fixed(stream));
    }
    public Quaternion(Fixed x, Fixed y, Fixed z, Fixed w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    @Override
    public String toString() {
        return "\"[ " + x + " , " + y + " , " + z + ", " + w + " ]\"";
    }
}
