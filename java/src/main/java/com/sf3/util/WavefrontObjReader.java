package com.sf3.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads wavefront obj files.
 * This reader does not keep any state, each interpreted line is directly send to the visitor.
 */
public class WavefrontObjReader {
    private static final String FLOAT = "[\\d\\.-]+";
    private static final Pattern VERTEX_PATTERN = Pattern.compile("\\s*v\\s+(?<x>" + FLOAT + ")\\s+(?<y>" + FLOAT + ")\\s+(?<z>" + FLOAT + ")");
    private static final Pattern TEXTURE_VERTEX_PATTERN = Pattern.compile("\\s*vt\\s+(?<u>" + FLOAT + ")\\s+(?<v>" + FLOAT + ")");

    private static final String INT = "[\\d]+";

    public static final Pattern FACE_PATTERN = Pattern.compile("\\s*f" +
            "\\s+" + faceGroup(1) +
            "\\s+" + faceGroup(2) +
            "\\s+" + faceGroup(3));

    private static String faceGroup(int index) {
        return "(?<v" + index + ">" + INT + ")(/(?<t"+index+">"+INT+"))?";
    }

    public void read(Reader reader, Visitor visitor) throws IOException {
        BufferedReader lineReader = new BufferedReader(reader);

        lineReader.lines().forEach(line -> parseLine(visitor, line));
    }

    private void parseLine(Visitor visitor, String lineWithComment) {
        // remove comments ( starting with #)
        int commentStart = lineWithComment.indexOf('#');
        String line = (commentStart < 0 ? lineWithComment : lineWithComment.substring(0, commentStart)).trim();

        if (line.length() == 0) {
            // empty line
            return;
        }
        Matcher vertexMatcher = VERTEX_PATTERN.matcher(line);
        if (vertexMatcher.matches()) {
            float x = getFloat(vertexMatcher, "x");
            float y = getFloat(vertexMatcher, "y");
            float z = getFloat(vertexMatcher, "z");
            visitor.visitVertex(x,y,z);
            return;
        }
        Matcher textureVertexMatcher = TEXTURE_VERTEX_PATTERN.matcher(line);
        if (textureVertexMatcher.matches()) {
            float u = getFloat(textureVertexMatcher, "u");
            float v = getFloat(textureVertexMatcher, "v");
            visitor.visitTextureVertex(u,v);
            return;
        }
        Matcher faceMatcher = FACE_PATTERN.matcher(line);
        if (faceMatcher.matches()) {
            int v1 = getInt(faceMatcher, "v1");
            int v2 = getInt(faceMatcher, "v2");
            int v3 = getInt(faceMatcher, "v3");
            int t1 = getInt(faceMatcher, "t1", -1);
            int t2 = getInt(faceMatcher, "t2", -1);
            int t3 = getInt(faceMatcher, "t3", -1);
            visitor.visitFace(
                    new FaceIndices(v1, t1, -1),
                    new FaceIndices(v2, t2, -1),
                    new FaceIndices(v3, t3, -1));
            return;
        }
        throw new IllegalStateException("unknown line: "+line);
    }

    private Integer getInt(Matcher faceMatcher, String groupName) {
        return Integer.valueOf(faceMatcher.group(groupName));
    }

    private Integer getInt(Matcher faceMatcher, String groupName, int defaultValue) {
        String value = faceMatcher.group(groupName);
        return value == null ? defaultValue : Integer.valueOf(value);
    }

    private float getFloat(Matcher vertexMatcher, String groupName) {
        return Float.parseFloat(vertexMatcher.group(groupName));
    }

    public interface  Visitor {
        void visitObject(String obj);
        void visitGroup(String group);
        void visitVertex(float x, float y, float z);
        void visitNormalVertex(float x, float y, float z);
        void visitTextureVertex(float u, float v);
        void visitFace(FaceIndices v0, FaceIndices v1, FaceIndices v2);
        void visitLine(int ... indices);
        void visitPoints(int ... indices);
        void visitMaterial(String materialFile);
    }

    /** No-Op implementation of the visitor interface. Just override the methods you need. */
    public static class VisitorAdapter implements Visitor {

        @Override
        public void visitObject(String obj) {
        }

        @Override
        public void visitGroup(String group) {
        }

        @Override
        public void visitVertex(float x, float y, float z) {
        }

        @Override
        public void visitNormalVertex(float x, float y, float z) {
        }

        @Override
        public void visitTextureVertex(float u, float v) {
        }

        @Override
        public void visitFace(FaceIndices v0, FaceIndices v1, FaceIndices v2) {
        }

        @Override
        public void visitLine(int... indices) {
        }

        @Override
        public void visitPoints(int... indices) {
        }

        @Override
        public void visitMaterial(String materialFile) {
        }
    }

    public static class FaceIndices {
        private final int vertexIndex;
        private final int textureIndex;
        private final int normalIndex;

        public FaceIndices(int vertexIndex, int textureIndex, int normalIndex) {
            this.vertexIndex = vertexIndex;
            this.textureIndex = textureIndex;
            this.normalIndex = normalIndex;
        }

        public int getVertexIndex() {
            return vertexIndex;
        }

        public int getTextureIndex() {
            return textureIndex;
        }

        public int getNormalIndex() {
            return normalIndex;
        }
    }
}
