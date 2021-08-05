package com.sf3.gamedata.sgl;

import com.sf3.gamedata.utils.Block;
import com.sf3.gamedata.utils.HexValue;
import com.sf3.util.ByteArrayImageInputStream;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteOrder;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Polygon Data Structure extended by another pointer.
 *
 * @see "SGL Strucure Reference, page 3, PDATA"
 */
public class PolygonData extends Block {

    public static final int SIZE = 20; // 0x14

    private final int numPoints;
    private final int pointsOffset;
    private List<Point> points;

    private final int numPolygons;
    private final int polygonOffset;
    private List<Polygon> polygons;

    private final int polygonAttributesOffset;
    private List<PolygonAttribute> polygonAttributes;

    public PolygonData(ImageInputStream stream, String name) throws IOException {
        this(stream, name, SIZE);
    }

    protected PolygonData(ImageInputStream stream, String name, int size) throws IOException {
        super(name, (int) stream.getStreamPosition(), size);
        this.pointsOffset = stream.readInt();
        this.numPoints = stream.readInt();
        this.polygonOffset = stream.readInt();
        this.numPolygons = stream.readInt();
        this.polygonAttributesOffset = stream.readInt();

        addProperty("pointsOffset", new HexValue(pointsOffset));
        addProperty("numPoints", numPoints);
        addProperty("polygonOffset", new HexValue(polygonOffset));
        addProperty("numPolygons", numPolygons);
        addProperty("polygonAttributesOffset", new HexValue(polygonAttributesOffset));
    }

    public void readDetails2(ImageInputStream stream, int chunkStart) throws IOException {
        stream.seek(pointsOffset + chunkStart);
        this.points = IntStream.range(0, numPoints).mapToObj(t -> readPoint(stream)).collect(Collectors.toList());

        stream.seek(polygonOffset + chunkStart);
        this.polygons = IntStream.range(0, numPolygons).mapToObj(t -> readPolygon(stream)).collect(Collectors.toList());

        stream.seek(polygonAttributesOffset + chunkStart);
        this.polygonAttributes = IntStream.range(0, numPolygons).mapToObj(t -> readPolygonAttribute(stream)).collect(Collectors.toList());
    }

    public void readDetails(ImageInputStream stream, int offset_after_header) throws IOException {
        // read Points
        stream.seek(pointsOffset - offset_after_header);
        this.points = IntStream.range(0, numPoints).mapToObj(t -> readPoint(stream)).collect(Collectors.toList());

        stream.seek(polygonOffset - offset_after_header);
        this.polygons = IntStream.range(0, numPolygons).mapToObj(t -> readPolygon(stream)).collect(Collectors.toList());

        stream.seek(polygonAttributesOffset - offset_after_header);
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

    public List<Point> getPoints() {
        return points;
    }

    public List<PolygonAttribute> getPolygonAttributes() {
        return polygonAttributes;
    }

    public List<Polygon> getPolygons() {
        return polygons;
    }

    public int getPointsOffset() {
        return pointsOffset;
    }

    public int getNumPoints() {
        return numPoints;
    }

    public int getPolygonOffset() {
        return polygonOffset;
    }

    public int getNumPolygons() {
        return numPolygons;
    }

    public int getPolygonAttributesOffset() {
        return polygonAttributesOffset;
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
