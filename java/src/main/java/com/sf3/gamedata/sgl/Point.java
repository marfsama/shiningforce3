package com.sf3.gamedata.sgl;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;

/**
 * A 3d point.
 *
 * @see "SGL Structure Reference, page 21"
 */
public class Point {
    /** Size in bytes of structure. */
    public static final int SIZE = Fixed.SIZE * 3;

    private final Fixed x;
    private final Fixed y;
    private final Fixed z;

    public Point(ImageInputStream stream) throws IOException {
        this(new Fixed(stream), new Fixed(stream), new Fixed(stream));
    }

    public Point(Fixed x, Fixed y, Fixed z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Fixed getX() {
        return x;
    }

    public Fixed getY() {
        return y;
    }

    public Fixed getZ() {
        return z;
    }

    @Override
    public String toString() {
        return "\"[ " + x +" , "+ y +" , " + z + " ]\"";
    }
}
