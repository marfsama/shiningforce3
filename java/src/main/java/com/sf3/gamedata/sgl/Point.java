package com.sf3.gamedata.sgl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sf3.gamedata.mpd.serializer.HexValueSerializer;
import com.sf3.gamedata.mpd.serializer.PointSerializer;
import lombok.SneakyThrows;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * A 3d point.
 *
 * @see "SGL Structure Reference, page 21"
 */
@JsonSerialize(using = PointSerializer.class)
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

    @SneakyThrows
    public Object toJson() {
        return Arrays.asList(x.toFloat(), y.toFloat(), z.toFloat());
    }
}
