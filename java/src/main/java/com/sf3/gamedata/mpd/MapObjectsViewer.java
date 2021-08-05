package com.sf3.gamedata.mpd;

import com.sf3.gamedata.utils.Block;
import com.sf3.gamedata.sgl.Angle;
import com.sf3.gamedata.sgl.Point;
import com.sf3.gamedata.sgl.PolygonData;
import com.sf3.simple3d.*;
import com.sf3.util.Sf3Util;
import com.sf3.util.StringUtil;
import com.sf3.util.WavefrontObjWriter;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class MapObjectsViewer {
    private int angleX;
    private int angleY;
    private int angleZ;

    private int textureCoordsIndex = 0;

    private static final Integer[][] textureCoordinateList = new Integer[][] {
            {0, 1, 2, 3},
            {0, 1, 3, 2},
            {0, 2, 3, 1},
            {0, 2, 1, 3},
            {0, 3, 1, 2},
            {0, 3, 2, 1},

            {1, 0, 2, 3},
            {1, 0, 3, 2},
            {1, 2, 0, 3},
            {1, 2, 3, 0},
            {1, 3, 0, 2},
            {1, 3, 2, 0},
    };

    public static void main(String[] args) {
        String basePath = System.getProperty("user.home")+"/project/games/shiningforce3/data/disk/mpd";
        String file = "sara04.mpd";
        Path path = Paths.get(basePath, file);

        Block mpdFile = new MpdReadWork().readFile(path, Paths.get("."));
        //new MapObjectsViewer().showModels(mpdFile);
        new MapObjectsViewer().showMap(mpdFile);
    }

    private void showMap(Block mpdFile) {
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        OrbitingCamera camera = new OrbitingCamera(0.5f);
        camera.setDistance(-1500.0f);
        camera.setPitch(25+180);
        RenderPanel renderPanel = new RenderPanel(camera);
        renderPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        renderPanel.setBackground(Color.BLACK);

        camera.addRenderPanel(renderPanel);

        JComponent textureSlider = createAngleSlider("Tex", textureCoordinateList.length-1, 1, (value) -> {
            this.textureCoordsIndex = value;
            renderPanel.setMesh(mapToMesh(mpdFile));
            renderPanel.repaint();
        });

        JComponent xSlider = createAngleSlider("X", 360, 5, (value) -> {
            this.angleX = value;
            renderPanel.setMesh(mapToMesh(mpdFile));
            renderPanel.repaint();
        });
        JComponent ySlider = createAngleSlider("Y", 360, 5, (value) -> {
            this.angleY = value;
            renderPanel.setMesh(mapToMesh(mpdFile));
            renderPanel.repaint();
        });
        JComponent zSlider = createAngleSlider("Z", 360, 5, (value) -> {
            this.angleZ = value;
            renderPanel.setMesh(mapToMesh(mpdFile));
            renderPanel.repaint();
        });


        JPanel southPanel = new JPanel(new GridLayout(0,1));
        southPanel.add(xSlider);
        southPanel.add(ySlider);
        southPanel.add(zSlider);
        southPanel.add(textureSlider);
        frame.add(southPanel, BorderLayout.SOUTH);


        frame.add(renderPanel, BorderLayout.CENTER);
        frame.setSize(800, 800);
        frame.setVisible(true);

        renderPanel.setMesh(mapToMesh(mpdFile));
    }

    private Mesh mapToMesh(Block mpdFile) {
        List<ModelHead> models = mpdFile.getBlock("map_objects").getBlock("models").getProperties().entrySet().stream()
                .filter(entry -> entry.getKey().startsWith("modelhead"))
                .map(entry -> (ModelHead) entry.getValue())
                .filter(modelHead -> modelHead.getPolygonData() != null)
                .collect(Collectors.toList());
        Map<Integer, Texture> texturesMap = mpdFile.getObject("textures");
        Map<Integer, BufferedImage> textures = texturesMap.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getImage()));

        Mesh mesh = polygonDataToMesh(models, textures);
        Block surfaceTiles = mpdFile.getBlock("surface_tiles");
        if (surfaceTiles.hasProperty("tiles")) {
            Tile<Tile<Integer>> tiles = surfaceTiles.getObject("tiles");
            List<Integer> textureCoords = Arrays.asList(0, 1, 3, 2);

            Matrix4 mirror = Matrix4.mirrorX();
            Matrix4 translate = Matrix4.translate(-1024+24, 0, -1024-24);
            Matrix4 rotate = createRotationMatrix(angleX, angleY, angleZ);

            Matrix4 transform = translate.multiply(mirror).multiply(rotate);
            for (int x = 0; x < 64; x++) {
                for (int y = 0; y < 64; y++) {
                    Integer value = tiles.get(x / 4, y / 4).get(x % 4, y % 4);
                    Texture texture = texturesMap.get(value);
                    Vector3 p0 = new Vector3(((x+0) * 32.0f), 0, (y+0) * 32.0f);
                    Vector3 p1 = new Vector3(((x+0) * 32.0f), 0, (y+1) * 32.0f);
                    Vector3 p2 = new Vector3(((x+1) * 32.0f), 0, (y+1) * 32.0f);
                    Vector3 p3 = new Vector3(((x+1) * 32.0f), 0, (y+0) * 32.0f);

                    int v0 = mesh.addVertex(transform.multiplyVector(p0));
                    int v1 = mesh.addVertex(transform.multiplyVector(p1));
                    int v2 = mesh.addVertex(transform.multiplyVector(p2));
                    int v3 = mesh.addVertex(transform.multiplyVector(p3));
                    if (texture != null) {
                        mesh.tris.add(new Triangle(v0, v1, v2, textureCoords.get(0), textureCoords.get(1), textureCoords.get(2), texture.getImage()));
                        mesh.tris.add(new Triangle(v2, v3, v0, textureCoords.get(2), textureCoords.get(3), textureCoords.get(0), texture.getImage()));
                    }
                }
            }

        }

        return mesh;
    }


    private void showModels(Block mpdFile) {
        List<ModelHead> models = mpdFile.getBlock("map_objects").getProperties().entrySet().stream()
                .filter(entry -> entry.getKey().startsWith("modelhead"))
                .map(entry -> (ModelHead) entry.getValue())
                .filter(modelHead -> modelHead.getPolygonData() != null)
                .collect(Collectors.toList());
        Map<Integer, Texture> texturesMap = mpdFile.getObject("textures");
        Map<Integer, BufferedImage> textures = texturesMap.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getImage()));


        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        OrbitingCamera camera = new OrbitingCamera(0.5f);
        camera.setDistance(-1500.0f);
        camera.setPitch(25+180);
        RenderPanel renderPanel = new RenderPanel(camera);
//        RenderPanel renderPanel2 = new RenderPanel(camera);
//        RenderPanel renderPanel3 = new RenderPanel(camera);
//        RenderPanel renderPanel4 = new RenderPanel(camera);
        renderPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
//        renderPanel2.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
//        renderPanel3.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
//        renderPanel4.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));

        JPanel renderFrame = new JPanel();
        renderFrame.setLayout(new GridLayout(1, 1));
        renderFrame.add(renderPanel);
//        renderFrame.add(renderPanel2);
//        renderFrame.add(renderPanel3);
//        renderFrame.add(renderPanel4);

        camera.addRenderPanel(renderPanel);
//        camera.addRenderPanel(renderPanel2);
//        camera.addRenderPanel(renderPanel3);
//        camera.addRenderPanel(renderPanel4);

        JSlider modelSlider = new JSlider(JSlider.HORIZONTAL, 0, models.size()-1, 0);
        modelSlider.setMinorTickSpacing(1);
        modelSlider.setMajorTickSpacing(5);
        modelSlider.setSnapToTicks(true);
        modelSlider.setPaintTicks(true);
        modelSlider.setPaintLabels(true);
        modelSlider.addChangeListener(e -> {
            System.out.println("Model: "+modelSlider.getValue());
            renderPanel.setMesh(polygonDataToMesh(models.get(modelSlider.getValue()), textures));
        });

        JComponent xSlider = createAngleSlider("X", 360, 5, (value) -> {
            this.angleX = value;
            renderPanel.setMesh(polygonDataToMesh(models, textures));
            renderPanel.repaint();
        });
        JComponent ySlider = createAngleSlider("Y", 360, 5, (value) -> {
            this.angleY = value;
            renderPanel.setMesh(polygonDataToMesh(models, textures));
            renderPanel.repaint();
        });
        JComponent zSlider = createAngleSlider("Z", 360, 5, (value) -> {
            this.angleZ = value;
            renderPanel.setMesh(polygonDataToMesh(models, textures));
            renderPanel.repaint();
        });


        JPanel southPanel = new JPanel(new GridLayout(0,1));
//        southPanel.add(modelSlider);
        southPanel.add(xSlider);
        southPanel.add(ySlider);
        southPanel.add(zSlider);
        frame.add(southPanel, BorderLayout.SOUTH);


        frame.add(renderFrame, BorderLayout.CENTER);
        frame.setSize(800, 800);
        frame.setVisible(true);

//        renderPanel.setMesh(polygonDataToMesh(models.get(160), textures));
//        renderPanel2.setMesh(polygonDataToMesh(models.get(216), textures));
//        renderPanel3.setMesh(polygonDataToMesh(models.get(1089), textures));
//        renderPanel4.setMesh(polygonDataToMesh(models.get(1189), textures));

//        renderPanel.setMesh(polygonDataToMesh(models, textures, 1089, 1090 ));
        renderPanel.setMesh(polygonDataToMesh(models, textures ));
    }

    private JComponent createAngleSlider(String name, int max, int spacing, Consumer<Integer> updater) {
        JSlider angleSlider = new JSlider(JSlider.HORIZONTAL, 0, max, 0);
        angleSlider.setMinorTickSpacing(spacing);
        angleSlider.setMajorTickSpacing(spacing*3);
        angleSlider.setSnapToTicks(true);
        angleSlider.setPaintTicks(true);
        angleSlider.setPaintLabels(true);
        angleSlider.addChangeListener(e -> {
            updater.accept(angleSlider.getValue());
        });

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
        panel.add(new JLabel(name+":"));
        panel.add(angleSlider);
        return panel;
    }

    private Mesh polygonDataToMesh(List<ModelHead> models, Map<Integer, BufferedImage> textures) {
        Mesh mesh = new Mesh();
        mesh.addTextureVertices(Sf3Util.texture_vertices);
        for (ModelHead modelHead : models) {
            PolygonData polygonData = modelHead.getPolygonData().get(0);
            Point position = modelHead.getObject("position");
            List<Angle> rotation = modelHead.getObject("rotation");
            Matrix4 translate = Matrix4.translate(position.getX().toFloat()+1000, position.getY().toFloat(), position.getZ().toFloat()+1000);
            Matrix4 rotationMatrix = createRotationMatrix(rotation.get(0).getDegree(), rotation.get(1).getDegree(), rotation.get(2).getDegree());
            Matrix4 rotationMatrix2 = createRotationMatrix(angleX, angleY, angleZ);
            Matrix4 transform = rotationMatrix.multiply(translate).multiply(rotationMatrix2);

            Sf3Util.addPolyDataToMesh(mesh, polygonData, transform, textures::get);
        }
        return mesh;
    }

    private Mesh polygonDataToMesh(List<ModelHead> models, Map<Integer, BufferedImage> textures, int ... modelIds) {
        Mesh mesh = new Mesh();
        mesh.addTextureVertices(Sf3Util.texture_vertices);
        for (int modelId : modelIds) {
            ModelHead modelHead = models.get(modelId);
            PolygonData polygonData = modelHead.getPolygonData().get(0);
            Point position = modelHead.getObject("position");
            List<Angle> rotation = modelHead.getObject("rotation");
            Matrix4 translate = Matrix4.translate(position.getX().toFloat(), position.getY().toFloat(), position.getZ().toFloat()+1000);
            Matrix4 rotationMatrix = createRotationMatrix(rotation.get(0).getDegree(), rotation.get(1).getDegree(), rotation.get(2).getDegree());
            Matrix4 rotationMatrix2 = createRotationMatrix(angleX, angleY, angleZ);
            Matrix4 transform = rotationMatrix.multiply(translate).multiply(rotationMatrix2);

            Sf3Util.addPolyDataToMesh(mesh, polygonData, transform, textures::get);
        }
        return mesh;
    }

    private Matrix4 createRotationMatrix(float rotX, float rotY, float rotZ) {
        Matrix4 rotateX = Matrix4.rotateX((float)Math.toRadians(rotX));
        Matrix4 rotateY = Matrix4.rotateY((float)Math.toRadians(rotY));
        Matrix4 rotateZ = Matrix4.rotateZ((float)Math.toRadians(rotZ));
        Matrix4 rotationMatrix = rotateZ.multiply(rotateY).multiply(rotateX);
        return rotationMatrix;
    }

    private Mesh polygonDataToMesh(ModelHead modelHead, Map<Integer, BufferedImage> textures) {
        Mesh mesh = new Mesh();
        mesh.addTextureVertices(Sf3Util.texture_vertices);
        PolygonData polygonData = modelHead.getPolygonData().get(0);
        return Sf3Util.addPolyDataToMesh(mesh, polygonData, textures::get);
    }

    private void dumpModelToWavefrontObj(Block mpdFile) {
        String filename = mpdFile.getName()+".object";
        List<PolygonData> models = mpdFile.getBlock("map_objects").getProperties().entrySet().stream()
                .filter(entry -> entry.getKey().startsWith("modelhead"))
                .map(entry -> (ModelHead) entry.getValue())
                .filter(modelHead -> modelHead.getPolygonData() != null)
                .map(modelHead -> modelHead.getPolygonData().get(0))
                .collect(Collectors.toList());

        for (int i = 0; i < models.size(); i++){
            WavefrontObjWriter obj = new WavefrontObjWriter();
            String name = "mesh[" + i + "]";
            PolygonData mesh = models.get(i);
            obj.nextGroup(name);
            mesh.getPoints().stream().forEach(p -> obj.addVertex(p.getX().toFloat(), p.getY().toFloat(), p.getZ().toFloat()));
            mesh.getPolygons().stream().forEach(polygon -> {
                        List<Integer> vertices = Arrays.stream(polygon.getVertexIndices()).mapToObj(Integer::valueOf).collect(Collectors.toList());
                        obj.addFace(vertices);
                    }
            );

            try {
                Files.write(Paths.get(filename+ StringUtil.getAsZerofilledString(i,2)+".obj"), obj.toString().getBytes());
                System.out.println("written to "+filename+i+".obj");
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }


}
