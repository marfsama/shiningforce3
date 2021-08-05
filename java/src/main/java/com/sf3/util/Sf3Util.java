package com.sf3.util;

import com.sf3.gamedata.sgl.*;
import com.sf3.gamedata.sgl.Polygon;
import com.sf3.simple3d.*;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.List;
import java.util.*;
import java.util.function.IntFunction;
import java.util.stream.Collectors;

public class Sf3Util {
    private static final int COLOR_OPAQUE = 0xFF << 24;

    private Sf3Util() {
    }

    /** converts 16 bit rgb (saturn bgr555) to 8 bits each channel (rgb888). */
    public static int rgb16ToRgb24(int value) {
        int r = (value & 0x1f) << 3;
        int g = ((value >> 5) & 0x1f) << 3;
        int b = ((value >> 10) & 0x1f) << 3;
        return (r << 16) + (g << 8) + b;
    }

    /** converts 24 bit rgb (rgb888) to saturn 16bit (bgr555). */
    public static int rgb24ToRgb16(int value) {
        int r = ((value >> 16) & 0xff) >> 3;
        int g = ((value >> 8) & 0xff) >> 3;
        int b = ((value) & 0xff) >> 3;
        // note: msb (bit 15) always is 1
        return (b << 10) + (g << 5) + r + (1 << 15);
    }

    /** Reads a plain 16bit Texture image into a {@link BufferedImage}. */
    public static BufferedImage readBufferedImage(ImageInputStream imageStream, int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int value = imageStream.readUnsignedShort();
                int color = (value == 0) ? 0 : Sf3Util.rgb16ToRgb24(value) + COLOR_OPAQUE;
                image.setRGB(x, y, color);
            }
        }
        return image;
    }

    private static final class TextureColumn {
        private int maxWidth = 0;
        private List<BufferedImage> images = new ArrayList<>();

        public void addImage(BufferedImage image) {
            images.add(image);
            maxWidth = Math.max(maxWidth, image.getWidth());
        }

        public int getMaxWidth() {
            return maxWidth;
        }

        public List<BufferedImage> getImages() {
            return images;
        }
    }

    public static List<TextureUv> writeTextureImage(List<BufferedImage> textures, Set<Integer> solidColors, String textureFileName, int textureIndexStart, boolean annotate) {
        if (textures.isEmpty() && solidColors.isEmpty()) {
            return Collections.emptyList();
        }

        int maxHeight = 512;
        int solidColorsSize = 8;
        List<TextureUv> uvs = new ArrayList<>();
        // segment textures in Columns with max <maxHeight> height
        List<TextureColumn> textureColumns = new ArrayList<>();
        int currentHeight = 0;
        // add texture images
        TextureColumn currentColumn = null;
        for (BufferedImage image : textures) {
            if (currentColumn == null || (currentHeight + image.getHeight() > maxHeight)) {
                currentColumn = new TextureColumn();
                textureColumns.add(currentColumn);
                currentHeight = 0;
            }
            currentHeight += image.getHeight();
            currentColumn.addImage(image);
        }
        // add solid colors
        int remainingColors = solidColors.size();
        // remove colors which fit into current column
        remainingColors -= (maxHeight - currentHeight) / solidColorsSize;
        // how many more columns do we need to represent the solid colors?
        int colorsFittingInColumn = maxHeight / solidColorsSize;
        int additionalColumns = (remainingColors + colorsFittingInColumn - 1) / colorsFittingInColumn;
        // if we don't have a texture column and still need some space for solid colors
        if (currentHeight == 0 && solidColors.isEmpty()) {
            // allocate an additional column
            additionalColumns++;
        }

        int totalWidth = textureColumns.stream().map(TextureColumn::getMaxWidth).mapToInt(Integer::valueOf).sum();

        BufferedImage image = new BufferedImage(totalWidth + additionalColumns * solidColorsSize, maxHeight, BufferedImage.TYPE_INT_RGB);
        Graphics graphics = image.getGraphics();
        graphics.setColor(Color.WHITE);
        int textureIndex = 0;
        int fontHeight = graphics.getFontMetrics().getHeight();
        int y = 0;
        int x = 0;
        for (int colIndex = 0; colIndex < textureColumns.size(); colIndex++) {
            TextureColumn column = textureColumns.get(colIndex);
            y = 0;
            for (BufferedImage texture : column.getImages()) {
                graphics.drawImage(texture, x,y, null);
                uvs.add(new TextureUv (1.0f * x / image.getWidth(), 1.0f * y / image.getHeight(),
                        (1.0f * x + texture.getWidth()) / image.getWidth(), (1.0f * y + texture.getHeight()) / image.getHeight()));
                if (annotate) {
                    graphics.drawRect(x, y, texture.getWidth(), texture.getHeight());
                    if (textureIndexStart >= 0) {
                        graphics.drawString(String.format("%02X", textureIndexStart + textureIndex), x, y + fontHeight);
                    }
                }
                y += texture.getHeight();
                textureIndex++;
            }
            // don't go to next column on last (and possibly not full) column
            if (colIndex < textureColumns.size() - 1) {
                x += column.getMaxWidth();
            }
        }
        int currentWidth = textureColumns.stream()
                .reduce((first, second) -> first)
                .map(TextureColumn::getMaxWidth)
                .stream().mapToInt(Integer::intValue)
                .findFirst()
                .orElseGet(() -> 0);
        for (int color : solidColors) {
            int rgb = Sf3Util.rgb16ToRgb24(color);
            graphics.setColor(new Color(rgb));
            if (y + solidColorsSize > maxHeight) {
                x += currentWidth;
                currentWidth = solidColorsSize;
                y = 0;
            }
            graphics.fillRect(x, y, solidColorsSize, solidColorsSize);
            uvs.add(new TextureUv (1.0f * x / image.getWidth(), 1.0f * y / image.getHeight(),
                    (1.0f * x + solidColorsSize) / image.getWidth(), (1.0f * y + solidColorsSize) / image.getHeight()));

            y += solidColorsSize;
        }
        graphics.dispose();
        try {
            ImageIO.write(image, "png", new File(textureFileName));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return uvs;
    }

    private static final Vector2 v0 = new Vector2(0.0f, 0.0f);
    private static final Vector2 v1 = new Vector2(0.0f, 1.0f);
    private static final Vector2 v2 = new Vector2(1.0f, 0.0f);
    private static final Vector2 v3 = new Vector2(1.0f, 1.0f);

    public static final List<Vector2> texture_vertices = Arrays.asList(v0, v1, v2, v3);

    protected static final List<Integer> textureCoordinatesNoFlip = Arrays.asList(0, 2, 3, 1);
    protected static final List<Integer> textureCoordinatesMirrorX = Arrays.asList(2, 0, 1, 3);
    protected static final List<Integer> textureCoordinatesMirrorY = Arrays.asList(1, 3, 2, 0);
    protected static final List<Integer> textureCoordinatesMirrorXY = Arrays.asList(3, 1, 0, 2);

    private static final List<List<Integer>> textureCoordinates = Arrays.asList(
            textureCoordinatesNoFlip,   // 00
            textureCoordinatesMirrorX,  // 01
            textureCoordinatesMirrorY,  // 10
            textureCoordinatesMirrorXY  // 11
    );

    public static Mesh addPolyDataToMesh(Mesh mesh, PolygonData polygonData, IntFunction<BufferedImage> textures) {
        return addPolyDataToMesh(mesh, polygonData, Matrix4.identity(), textures);
    }
    public static Mesh addPolyDataToMesh(Mesh mesh, PolygonData polygonData, Matrix4 transform, IntFunction<BufferedImage> textures) {

        for (int i = 0; i < polygonData.getPolygons().size(); i++) {
            Polygon polygon = polygonData.getPolygons().get(i);
            PolygonAttribute attribute = polygonData.getPolygonAttributes().get(i);


            List<Vector3> vertices = Arrays.stream(polygon.getVertexIndices())
                    .mapToObj(index -> polygonData.getPoints().get(index))
                    .map(p -> new Vector3(p.getX().toFloat(), p.getY().toFloat(), p.getZ().toFloat()))
                    .collect(Collectors.toList());
            int v0 = mesh.addVertex(transform.multiplyVector(vertices.get(0)));
            int v1 = mesh.addVertex(transform.multiplyVector(vertices.get(1)));
            int v2 = mesh.addVertex(transform.multiplyVector(vertices.get(2)));
            int v3 = mesh.addVertex(transform.multiplyVector(vertices.get(3)));
            int drawMode = attribute.getDir() & 0x0f;
            if (drawMode == PolygonAttribute.DRAW_MODE_TEXTURED) {
                int textureId = attribute.getTexno();
                BufferedImage textureImage = textures.apply(textureId);
                List<Integer> textureCoords = textureCoordinates.get((attribute.getDir() >> 4) & 0x3);
                mesh.tris.add(new Triangle(v0, v1, v2, textureCoords.get(0), textureCoords.get(1), textureCoords.get(2), textureImage));
                mesh.tris.add(new Triangle(v2, v3, v0, textureCoords.get(2), textureCoords.get(3), textureCoords.get(0), textureImage));
            }
            else if (drawMode == PolygonAttribute.DRAW_MODE_POLY) {
                int color = Sf3Util.rgb16ToRgb24(attribute.getColno());
                mesh.tris.add(new Triangle(v0, v1, v2, color));
                mesh.tris.add(new Triangle(v2, v3, v0, color));
            }
        }
        return mesh;
    }

    /** Reads an unsigned byte from the stream, rethrowing a possible {@link IOException} as an unchecked exception. */
    public static Integer readUnsignedByte(ImageInputStream stream) {
        try {
            return stream.readUnsignedByte();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Reads an unsigned int from the stream, rethrowing a possible {@link IOException} as an unchecked exception. */
    public static Integer readInt(ImageInputStream stream) {
        try {
            return stream.readInt();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Reads an unsigned short from the stream, rethrowing a possible {@link IOException} as an unchecked exception. */
    public static Integer readUnsignedShort(ImageInputStream stream) {
        try {
            return stream.readUnsignedShort();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Reads a short from the stream, rethrowing a possible {@link IOException} as an unchecked exception. */
    public static Short readShort(ImageInputStream stream) {
        try {
            return stream.readShort();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static Fixed readCompressedSglFixed(ImageInputStream stream) {
        // read 16 bits signed
        short s = readShort(stream);
        // sign extend to 32 bits
        int i = (int) s;
        // scale to correct value
        int scaled = i << 2;
        return new Fixed(scaled);
    }

    public static Fixed readSglFixed(ImageInputStream stream) {
        return new Fixed(readInt(stream));
    }

    public static List<String> getExceptionLines(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        BufferedReader br = new BufferedReader(new StringReader(sw.toString()));
        return br.lines().collect(Collectors.toList());
    }
}
