package com.sf3.util;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class WavefrontObjWriter {
    private String name = "dummy";
    private List<Point> vertices = new ArrayList<>();
    private List<Point> texture = new ArrayList<>();

    private int vertexIndexStart = 1;
    private int textureIndexStart = 1;

    private List<Point> normals = new ArrayList<>();
    private List<Point> textureVerts = new ArrayList<>();

    private List<Object> primitives = new ArrayList<>();

    public void addVertex(float x, float y, float z) {
        vertices.add(new Point("v",x,y,z));
    }

    public int addTextureVertex(float x, float y) {
        textureVerts.add(new Point("vt",x,y,0));
        return textureVerts.size() - textureIndexStart;
    }

    public void addFace(List<Integer> points) {
        primitives.add(new Face(points, vertexIndexStart, null, 0));
    }

    public void addFace(List<Integer> points, List<Integer> textureVertices) {
        primitives.add(new Face(points, vertexIndexStart, textureVertices, textureIndexStart));
    }

    public void setMaterialLibrary(String filename) {
        primitives.add(new Material("mtllib", filename));
    }
    public void setMaterial(String name) {
        primitives.add(new Material("usemtl", name));
    }

    public void nextGroup(String name) {
        primitives.add(new Group(name));
        vertexIndexStart = vertices.size() + 1;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder("o "+name);
        buf.append("\n");

        buf.append(vertices.stream().map(String::valueOf).collect(Collectors.joining("\n")));
        buf.append("\n");
        buf.append(textureVerts.stream().map(String::valueOf).collect(Collectors.joining("\n")));
        buf.append("\n");

        buf.append(primitives.stream().map(String::valueOf).collect(Collectors.joining("\n")));
        buf.append("\n");

        return buf.toString();
    }

    public static class Group {
        private final String name;

        public Group(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return "o " + name;
        }
    }

    public static class Material {
        private final String material;
        private final String type;

        public Material(String type, String material) {
            this.material = material;
            this.type = type;
        }

        @Override
        public String toString() {
            return type+" "+material;
        }
    }
    public static class Face {
        private final List<Integer> points;
        private final int vertexIndexStart;
        private final List<Integer> texture;
        private final int textureIndexStart;

        public Face(List<Integer> points, int vertexIndexStart, List<Integer> texture, int textureIndexStart) {
            this.points = points;
            this.vertexIndexStart = vertexIndexStart;
            this.texture = texture;
            this.textureIndexStart = textureIndexStart;
        }

        @Override
        public String toString() {
            StringBuilder buf = new StringBuilder("f ");
            for (int i = 0; i < points.size(); i++) {
                buf.append(points.get(i)+vertexIndexStart);
                if (texture != null) {
                    buf.append("/"+(texture.get(i)+textureIndexStart));
                }
                buf.append(" ");
            }

            return buf.toString();
        }
    }

    public static class Point {
        private final String type;
        private final float x;
        private final float y;
        private final float z;

        public Point(String type, float x, float y, float z) {
            this.type = type;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public String toString() {
            return type + ' '+ x + ' ' + y + ' '+z;
        }
    }
}
