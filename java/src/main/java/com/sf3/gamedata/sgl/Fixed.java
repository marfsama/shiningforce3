package com.sf3.gamedata.sgl;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.util.Locale;

/**
 * Fixed point 32 bit numbers.
 *
 * @see "SGL Programmers Tutorial, page 1-8"
 * @see "SGL Structure Reference, page 19"
 */
public class Fixed {
    /** Structure size in bytes. */
    public static final int SIZE = 4;
    /** Raw fixed point value. */
    private final int value;

    public Fixed(ImageInputStream stream) throws IOException {
        this(stream.readInt());
    }

    public Fixed(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public float toFloat() {
        return value / 65536.0f;
    }

    public double toDouble() {
        return value / 65536.0;
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT, "%.4f",toFloat());
    }
}
