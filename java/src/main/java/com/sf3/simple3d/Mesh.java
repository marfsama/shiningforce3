package com.sf3.simple3d;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class Mesh {
    private static final AtomicInteger meshCounter = new AtomicInteger(0);

    public String name;
    public List<Vector3> vertices = new ArrayList<>();
    public List<Vector2> textureVertex = new ArrayList<>();

    public List<Triangle> tris = new ArrayList<>();

    public Mesh() {
        this("mesh"+meshCounter.getAndIncrement());
    }

    public Mesh(String name) {
        this.name = name;
    }

    public int addVertex(float x, float y, float z) {
        return addVertex(new Vector3(x,y,z));
    }

    public int addVertex(Vector3 vertex) {
        int index = vertices.indexOf(vertex);
        if (index >= 0)
            return index;
        vertices.add(vertex);
        return vertices.size()-1;
    }

    /**
     * Adds a vertex. When this vertex already exists, return the existing index, otherwise add the vertex and return
     * the new index.
     */
    public int addVertexWithDuplicatesCheck(Vector3 vertex) {
        int index = vertices.indexOf(vertex);
        if (index < 0) {
            index = vertices.size();
            vertices.add(vertex);
        }
        return index;
    }

    public int addTextureVertex(float u, float v) {
        textureVertex.add(new Vector2(u,v));
        return textureVertex.size() - 1;
    }

    public void addTextureVertices(List<Vector2> vertices) {
        textureVertex.addAll(vertices);
    }
}