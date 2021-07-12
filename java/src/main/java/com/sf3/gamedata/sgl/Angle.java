package com.sf3.gamedata.sgl;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;

/**
 * Fixed point 32 bit numbers.
 *
 * @see "SGL Structure Reference, page 18"
 */
public class Angle {
    /** Structure size in bytes. */
    public static final int SIZE = 2;
    /** Raw  value. */
    private final int value;

    public Angle(ImageInputStream stream) throws IOException {
        this(stream.readUnsignedShort());
    }

    public Angle(int value) {
        this.value = value;
    }

    /** returns the raw value. */
    public int getValue() {
        return value;
    }

    /** Returns the angle in degree (0..360)*/
    public float getDegree() {
        return value / (65536.0f / 360.0f);
    }

    /** Returns the angle in radians (0..2 PI)*/
    public float getRadians() {
        return (float) Math.toRadians(getDegree());
    }

    @Override
    public String toString() {
        return ""+getDegree()+"°";
    }
}
