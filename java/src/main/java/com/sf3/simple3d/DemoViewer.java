package com.sf3.simple3d;

import com.sf3.util.WavefrontObjReader;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class DemoViewer {

    public static void main(String[] args) {
        JFrame frame = new JFrame();
        frame.setLayout(new BorderLayout());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        OrbitingCamera camera = new OrbitingCamera(1.5f);
        camera.setDistance(100.0f);

        // panel to display render results
        RenderPanel renderPanel = new RenderPanel(camera);
        camera.addRenderPanel(renderPanel);
        renderPanel.setMesh(createSphereModel());
        frame.add(renderPanel);

        frame.setSize(400, 400);
        frame.setVisible(true);
    }

    private static Mesh createCowMesh() {
        Cow cow = new Cow();
        try (InputStream cowStream = DemoViewer.class.getResourceAsStream("/cow.obj")) {
            new WavefrontObjReader().read(new InputStreamReader(cowStream), cow);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return cow.mesh;
    }

    private static class Tris {
        Vector3 v1;
        Vector3 v2;
        Vector3 v3;
        Color color;


        public Tris(Vector3 v1, Vector3 v2, Vector3 v3, Color color) {
            this.v1 = v1;
            this.v2 = v2;
            this.v3 = v3;
            this.color = color;
        }
    }

    private static Mesh createSphereModel() {
        List<Tris> tris = new ArrayList<Tris>();
        tris.add(new Tris(new Vector3(10, 10, 10),
                new Vector3(-10, 10, -10),
                new Vector3(-10, -10, 10),
                Color.WHITE));
        tris.add(new Tris(new Vector3(10, 10, 10),
                new Vector3(-10, -10, 10),
                new Vector3(10, -10, -10),
                Color.RED));
        tris.add(new Tris(new Vector3(-10, 10, -10),
                new Vector3(10, 10, 10),
                new Vector3(10, -10, -10),
                Color.GREEN));
        tris.add(new Tris(new Vector3(-10, 10, -10),
                new Vector3(10, -10, -10),
                new Vector3(-10, -10, 10),
                Color.BLUE));

        for (int i = 0; i < 4; i++) {
            tris = inflate(tris);
        }

        // create mesh
        Mesh mesh = new Mesh();
        for (Tris t : tris) {
            int v1 = mesh.addVertexWithDuplicatesCheck(t.v1);
            int v2 = mesh.addVertexWithDuplicatesCheck(t.v2);
            int v3 = mesh.addVertexWithDuplicatesCheck(t.v3);
            Triangle triangle = new Triangle(v1, v2, v3);
            triangle.color = t.color.getRGB();
            mesh.tris.add(triangle);
        }

        System.out.println(mesh.vertices.size()+" vertices, "+mesh.tris.size()+" triangles");

        return mesh;
    }


    public static java.util.List<Tris> inflate(java.util.List<Tris> tris) {
        java.util.List<Tris> result = new ArrayList<>();
        for (Tris t : tris) {
            Vector3 m1 = new Vector3((t.v1.x + t.v2.x)/2, (t.v1.y + t.v2.y)/2, (t.v1.z + t.v2.z)/2);
            Vector3 m2 = new Vector3((t.v2.x + t.v3.x)/2, (t.v2.y + t.v3.y)/2, (t.v2.z + t.v3.z)/2);
            Vector3 m3 = new Vector3((t.v1.x + t.v3.x)/2, (t.v1.y + t.v3.y)/2, (t.v1.z + t.v3.z)/2);
            result.add(new Tris(t.v1, m1, m3, t.color));
            result.add(new Tris(t.v2, m1, m2, t.color));
            result.add(new Tris(t.v3, m2, m3, t.color));
            result.add(new Tris(m1, m2, m3, t.color));
        }

        for (Tris t : result) {
            t.v1 = t.v1.scale((float) (Math.sqrt(300) / t.v1.length()));
            t.v2 = t.v2.scale((float) (Math.sqrt(300) / t.v2.length()));
            t.v3 = t.v3.scale((float) (Math.sqrt(300) / t.v3.length()));
        }

        return result;
    }

}


