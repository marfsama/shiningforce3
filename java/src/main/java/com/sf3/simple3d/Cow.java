package com.sf3.simple3d;

import com.sf3.util.WavefrontObjReader;

public class Cow extends WavefrontObjReader.VisitorAdapter {
    public Mesh mesh = new Mesh();

    public Cow() {
        // add dummy index zero, as the face indexed all start at 1
        mesh.vertices.add(null);
        mesh.textureVertex.add(null);
    }

    @Override
    public void visitVertex(float x, float y, float z) {
        mesh.addVertex(x, y, z);
    }

    @Override
    public void visitTextureVertex(float u, float v) {
        mesh.addTextureVertex(u, v);
    }

    @Override
    public void visitFace(WavefrontObjReader.FaceIndices v0, WavefrontObjReader.FaceIndices v1, WavefrontObjReader.FaceIndices v2) {
        // note: wavefront files start indices at 1 instead of 0. In the constructor we
        // added a dummy vertex, so there is no need to adjust the indices here.
        mesh.tris.add(new Triangle(v0.getVertexIndex(), v1.getVertexIndex(), v2.getVertexIndex(),
                    v0.getTextureIndex(), v1.getTextureIndex(), v2.getTextureIndex(), null));
    }
}
