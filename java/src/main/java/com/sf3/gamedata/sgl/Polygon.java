package com.sf3.gamedata.sgl;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * Polygon description.
 *
 * @see "SGL Structure Reference, page 5"
 */
public class Polygon {
    /** Structure size in bytes. */
    public static final int SIZE = Point.SIZE + 4*2;

    private final Point normalVector;
    private final int[] vertexIndices;

    public Polygon(ImageInputStream stream) throws IOException {
        this.normalVector = new Point(new Fixed(stream.readInt()), new Fixed(stream.readInt()), new Fixed(stream.readInt()));
        this.vertexIndices = new int[]{stream.readShort(), stream.readShort(), stream.readShort(), stream.readShort()};
    }

    public Point getNormalVector() {
        return normalVector;
    }

    public int[] getVertexIndices() {
        return vertexIndices;
    }

    @Override
    public String toString() {
        return "{" +
                "\"normalVector\": " + normalVector +
                ", \"vertexIndices\": " + Arrays.toString(vertexIndices) +
                '}';
    }
}
