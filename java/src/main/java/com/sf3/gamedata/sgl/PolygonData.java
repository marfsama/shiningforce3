package com.sf3.gamedata.sgl;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Polygon Data Structure.
 *
 * @see "SGL Strucure Reference, page 3, PDATA"
 */
@Getter
public class PolygonData {

    public static final int SIZE = 20; // 0x14

    private final int numPoints;
    private final int numPolygons;

    @JsonIgnore
    private final int pointsOffset;
    @JsonIgnore
    private final int polygonOffset;
    @JsonIgnore
    private final int polygonAttributesOffset;


    private List<Point> points;
    private List<Polygon> polygons;
    private List<PolygonAttribute> polygonAttributes;

    public PolygonData(ImageInputStream stream) throws IOException {
        this.pointsOffset = stream.readInt();
        this.numPoints = stream.readInt();
        this.polygonOffset = stream.readInt();
        this.numPolygons = stream.readInt();
        this.polygonAttributesOffset = stream.readInt();
    }

    public void readDetails2(ImageInputStream stream, int chunkStart) throws IOException {
        stream.seek((long) pointsOffset + chunkStart);
        this.points = IntStream.range(0, numPoints).mapToObj(t -> readPoint(stream)).collect(Collectors.toList());

        stream.seek((long) polygonOffset + chunkStart);
        this.polygons = IntStream.range(0, numPolygons).mapToObj(t -> readPolygon(stream)).collect(Collectors.toList());

        stream.seek((long) polygonAttributesOffset + chunkStart);
        this.polygonAttributes = IntStream.range(0, numPolygons).mapToObj(t -> readPolygonAttribute(stream)).collect(Collectors.toList());
    }

    public void readDetails(ImageInputStream stream, int offsetAfterHeader) throws IOException {
        // read Points
        stream.seek((long) pointsOffset - offsetAfterHeader);
        this.points = IntStream.range(0, numPoints).mapToObj(t -> readPoint(stream)).collect(Collectors.toList());

        stream.seek((long) polygonOffset - offsetAfterHeader);
        this.polygons = IntStream.range(0, numPolygons).mapToObj(t -> readPolygon(stream)).collect(Collectors.toList());

        stream.seek((long) polygonAttributesOffset - offsetAfterHeader);
        this.polygonAttributes = IntStream.range(0, numPolygons).mapToObj(t -> readPolygonAttribute(stream)).collect(Collectors.toList());
    }

    private PolygonAttribute readPolygonAttribute(ImageInputStream stream) {
        try {
            return new PolygonAttribute(stream);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Polygon readPolygon(ImageInputStream stream) {
        try {
            return new Polygon(stream);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Point readPoint(ImageInputStream stream) {
        return new Point(readFixed(stream),readFixed(stream),readFixed(stream));
    }

    private Fixed readFixed(ImageInputStream stream) {
        try {
            return new Fixed(stream.readInt());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public String toString() {
        if (points == null) {
            return super.toString();
        }

        return "{" +
                "\"points\":" + points +
                ", \"polygons\":" + polygons +
                ", \"polygonAttributes\":" + polygonAttributes +
                '}';
    }
}
