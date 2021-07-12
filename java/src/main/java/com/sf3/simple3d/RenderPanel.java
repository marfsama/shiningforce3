package com.sf3.simple3d;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * @see "https://gist.github.com/Rogach/f3dfd457d7ddb5fcfd99/4f2aaf20a468867dc195cdc08a02e5705c2cc95c"
 * @see "https://www.scratchapixel.com/lessons/3d-basic-rendering/rasterization-practical-implementation"
 * */
public class RenderPanel extends JPanel {
    private Mesh mesh;

    private static final float inchToMm = 25.4f;
    private enum FitResolutionGate { kFill, kOverscan };


    public RenderPanel(Camera camera) {
        this.camera = camera;
    }

    public void setMesh(Mesh mesh) {
        this.mesh = mesh;
    }

    public static final class Screen {
        float fov;
        float top;
        float bottom;
        float left;
        float right;

        public float width() {
            return right - left;
        }

        public float height() {
            return top - bottom;
        }

        @Override
        public String toString() {
            return "Screen{" +
                    "top=" + top +
                    ", bottom=" + bottom +
                    ", left=" + left +
                    ", right=" + right +
                    '}';
        }
    }

    private Screen computeScreenCoordinates(
        float filmApertureWidth, float filmApertureHeight,
        int imageWidth, int imageHeight, FitResolutionGate fitFilm,
        float nearClippingPLane, float focalLength
    ) {
        Screen screen = new Screen();
        float filmAspectRatio = filmApertureWidth / filmApertureHeight;
        float deviceAspectRatio = imageWidth / (float)imageHeight;

        screen.top = ((filmApertureHeight * inchToMm / 2) / focalLength) * nearClippingPLane;
        screen.right = ((filmApertureWidth * inchToMm / 2) / focalLength) * nearClippingPLane;

        // field of view (horizontal)
        screen.fov = (float) (2 * 180 / Math.PI * Math.atan((filmApertureWidth * inchToMm / 2) / focalLength));

        float xscale = 1;
        float yscale = 1;

        if (filmAspectRatio > deviceAspectRatio) {
            yscale = filmAspectRatio / deviceAspectRatio;
        }
        else {
            xscale = deviceAspectRatio / filmAspectRatio;
        }

        screen.right *= xscale;
        screen.top *= yscale;

        screen.bottom = -screen.top;
        screen.left = -screen.right;
        return screen;
    }

    /**
     * Compute vertex raster screen coordinates. Vertices are defined in world space. They are then converted to camera
     * space, then to NDC space (in the range [-1,1]) and then to raster space. The z-coordinates of the vertex in
     * raster space is set with the z-coordinate of the vertex in camera space.
     */
    protected Vector3 convertToRaster(
            Vector3 vertexWorld,
            Matrix4 worldToCamera,
            Screen screen,
            float near,
            int imageWidth,
            int imageHeight,
            Matrix4 perspectiveMatrix) {
        Vector3 vertexCamera = worldToCamera.multiplyVector(vertexWorld);
        float r = screen.right;
        float l = screen.left;
        float t = screen.top;
        float b = screen.bottom;

        // convert to screen space (perspective divide)
        Vector2 vertexScreen = new Vector2(
                near * vertexCamera.x / -vertexCamera.z,
                near * vertexCamera.y / -vertexCamera.z);

        // now convert point from screen space to NDC space (in range [-1,1])
        Vector2 vertexNDC = new Vector2(
                2 * vertexScreen.x / (screen.width()) - (r + l) / (screen.width()),
                2 * vertexScreen.y / (screen.height()) - (t + b) / (screen.height()));

        return new Vector3(
            // convert to raster space
            (vertexNDC.x + 1) / 2 * imageWidth,
            // in raster space y is down so invert direction
            (1 - vertexNDC.y) / 2 * imageHeight,
            -vertexCamera.z);
    }

    private float min3(float a, float b, float c) {
        return Math.min(a, Math.min(b, c));
    }

    private float max3(float a, float b, float c) {
        return Math.max(a, Math.max(b, c));
    }

    private float edgeFunction(Vector3 a, Vector3 b, Vector3 p) {
        return (p.x - a.x) * (b.y - a.y) - (p.y - a.y) * (b.x - a.x);
    }

    private Camera camera;

    private float nearClippingPLane = 1;
    private float farClippingPlane = 2500;
    private float focalLength = 20; // in mm
    // 35mm Full Aperture in inches
    private float filmApertureWidth = 0.980f;
    private float filmApertureHeight = 0.735f;

    public void paintComponent(Graphics graphics) {
        Graphics2D g2 = (Graphics2D) graphics;
        g2.setColor(getBackground());
        g2.fillRect(0, 0, getWidth(), getHeight());

        if (mesh == null) {
            g2.setColor(Color.WHITE);
            g2.drawString("no mesh", 50, 50);
            return;
        }

        int imageWidth = getWidth();
        int imageHeight = getHeight();

        Matrix4 worldToCamera = camera.getTransform();
        Matrix4 cameraToWorld = worldToCamera.inverse();

        // compute screen coordinates
        Screen screen = computeScreenCoordinates(
                filmApertureWidth, filmApertureHeight,
                imageWidth, imageHeight,
                FitResolutionGate.kOverscan,
                nearClippingPLane,
                focalLength);

        // define the frame-buffer and the depth-buffer. Initialize depth buffer
        // to far clipping plane.
        BufferedImage framebuffer = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
        Graphics imageGraphics = framebuffer.getGraphics();
        imageGraphics.setColor(getBackground());
        imageGraphics.fillRect(0,0, imageWidth, imageHeight);
        imageGraphics.dispose();
        float[] depthBuffer = new float[imageWidth * imageHeight];
        // initialize array with extremely far away depths
        Arrays.fill(depthBuffer, farClippingPlane);

        long drawnTriangles = 0;
        long t_start = System.currentTimeMillis();

        Matrix4 perspectiveMatrix = Matrix4.perspective(nearClippingPLane, farClippingPlane,
                screen.left, screen.right, screen.top, screen.bottom);

        List<Vector3> vRaster = mesh.vertices.stream()
                // precompute raster coordinates for each vertex
                .map(v -> convertToRaster(v, worldToCamera, screen, nearClippingPLane, imageWidth, imageHeight, perspectiveMatrix))
                .map(v -> v.z(1.0f / v.z))
                .collect(Collectors.toList());

        List<Vector3> vCamera = mesh.vertices.stream()
                // precompute world to camera space for each vertex
                .map(v -> worldToCamera.multiplyVector(v))
                .map(v -> v.z(1.0f / -v.z))
                .collect(Collectors.toList());

        List<Vector3> vertices = mesh.vertices;

        // outer loop
        for (Triangle triangle : mesh.tris) {
/*            Vector3 v0 = mesh.vertices.get(triangle.v1);
            Vector3 v1 = mesh.vertices.get(triangle.v2);
            Vector3 v2 = mesh.vertices.get(triangle.v3);

            // Convert the vertices of the triangle to raster space
            Vector3 v0Raster = convertToRaster(v0, worldToCamera, screen, nearClippingPLane, imageWidth, imageHeight, perspectiveMatrix);
            Vector3 v1Raster = convertToRaster(v1, worldToCamera, screen, nearClippingPLane, imageWidth, imageHeight, perspectiveMatrix);
            Vector3 v2Raster = convertToRaster(v2, worldToCamera, screen, nearClippingPLane, imageWidth, imageHeight, perspectiveMatrix);
            // Precompute reciprocal of vertex z-coordinate
            v0Raster = v0Raster.z(1 / v0Raster.z);
            v1Raster = v1Raster.z(1 / v1Raster.z);
            v2Raster = v2Raster.z(1 / v2Raster.z);
*/
            Vector3 v0Raster = vRaster.get(triangle.v1);
            Vector3 v1Raster = vRaster.get(triangle.v2);
            Vector3 v2Raster = vRaster.get(triangle.v3);

            // Prepare vertex attributes. Divde them by their vertex z-coordinate (though we use a
            // multiplication here because v.z = 1 / v.z)
            Vector2 textureVertex0 = triangle.hasUV() ? mesh.textureVertex.get(triangle.t1).scale(v0Raster.z) : null;
            Vector2 textureVertex1 = triangle.hasUV() ? mesh.textureVertex.get(triangle.t2).scale(v1Raster.z) : null;
            Vector2 textureVertex2 = triangle.hasUV() ? mesh.textureVertex.get(triangle.t3).scale(v2Raster.z) : null;

            float xmin = min3(v0Raster.x, v1Raster.x, v2Raster.x);
            float ymin = min3(v0Raster.y, v1Raster.y, v2Raster.y);
            float xmax = max3(v0Raster.x, v1Raster.x, v2Raster.x);
            float ymax = max3(v0Raster.y, v1Raster.y, v2Raster.y);
            // the triangle is out of screen
            if (xmin > imageWidth - 1 || xmax < 0 || ymin > imageHeight - 1 || ymax < 0) {
                continue;
            }

            // update statistics
            drawnTriangles++;

            // clamp to screen
            int x0 = Math.max(0, (int)(Math.floor(xmin)));
            int x1 = Math.min(imageWidth - 1, (int)(Math.floor(xmax)));
            int y0 = Math.max(0, (int)(Math.floor(ymin)));
            int y1 = Math.min(imageHeight - 1, (int)(Math.floor(ymax)));

            float area = edgeFunction(v0Raster, v1Raster, v2Raster);

            // Inner loop
            for (int y = y0; y <= y1; ++y) {
                for (int x = x0; x <= x1; ++x) {
                    Vector3 pixelSample = new Vector3 (x + 0.5f, y + 0.5f, 0);
                    float w0 = edgeFunction(v1Raster, v2Raster, pixelSample);
                    float w1 = edgeFunction(v2Raster, v0Raster, pixelSample);
                    float w2 = edgeFunction(v0Raster, v1Raster, pixelSample);
                    if ((w0 >= 0 && w1 >= 0 && w2 >= 0) ||
                            (w0 <= 0 && w1 <= 0 && w2 <= 0)) {
                        w0 /= area;
                        w1 /= area;
                        w2 /= area;
                        float oneOverZ = v0Raster.z * w0 + v1Raster.z * w1 + v2Raster.z * w2;
                        float z = 1 / oneOverZ;
                        // Depth-buffer test
                        if (z < depthBuffer[y * imageWidth + x]) {
                            // texturePos = textureVertex0 * w0 + textureVertex1 * w1 + textureVertex2 * w2
                            Vector2 texturePos = textureVertex0 != null ? (textureVertex0.scale(w0).add(textureVertex1.scale(w1)).add(textureVertex2.scale(w2))).scale(z) : null;

                            // If you need to compute the actual position of the shaded point in camera space. Proceed
                            // like with the other vertex attribute. Divide the point coordinates by the vertex
                            // z-coordinate then interpolate using barycentric coordinates and finally multiply
                            // by sample depth.
                            /*
                            Vector3 v0Cam = worldToCamera.multiplyVector(vertices.get(triangle.v1));
                            Vector3 v1Cam = worldToCamera.multiplyVector(vertices.get(triangle.v2));
                            Vector3 v2Cam = worldToCamera.multiplyVector(vertices.get(triangle.v3));

                            float px = (v0Cam.x/-v0Cam.z) * w0 + (v1Cam.x/-v1Cam.z) * w1 + (v2Cam.x/-v2Cam.z) * w2;
                            float py = (v0Cam.y/-v0Cam.z) * w0 + (v1Cam.y/-v1Cam.z) * w1 + (v2Cam.y/-v2Cam.z) * w2;
*/
                            Vector3 v0Cam = vCamera.get(triangle.v1);
                            Vector3 v1Cam = vCamera.get(triangle.v2);
                            Vector3 v2Cam = vCamera.get(triangle.v3);

                            float px = (v0Cam.x) * w0 + (v1Cam.x) * w1 + (v2Cam.x) * w2;
                            float py = (v0Cam.y) * w0 + (v1Cam.y) * w1 + (v2Cam.y) * w2;

                            Vector3 pt = new Vector3(px * z, py * z, -z); // pt is in camera space
                            // Compute the face normal which is used for a simple facing ratio. Keep in mind that we
                            // are doing all calculation in camera space. Thus the view direction can be computed as
                            // the point on the object in camera space minus Vec3f(0), the position of the camera in
                            // camera space.
                            Vector3 normal = v1Cam.sub(v0Cam).crossProduct(v2Cam.sub(v0Cam)).normalize();
                            Vector3 viewDirection = pt.negate().normalize();

                            float nDotView =  Math.max(0.0f, normal.dotProduct(viewDirection));

                            BufferedImage textureImage = triangle.texture;
                            if (texturePos != null && textureImage != null) {
//                                float c = 0.3f * (1.0f - checker) + 0.7f * checker;

//                                int r = (int) (nDotView * c * 255.0f);
                                int tx = (int) (Math.abs(texturePos.x % 1.0f) * (textureImage.getWidth()-1));
                                int ty = (int) (Math.abs(texturePos.y % 1.0f) * (textureImage.getHeight()-1));
                                int color = textureImage.getRGB(tx, ty);
//                                int color = triangle.colorResolve.apply(texturePos);
                                // only draw pixel when it is not transparent
                                if (((color >> 24) & 0xff) > 0) {
                                    /*
                                    float colorScale = 0.5f + nDotView * 0.5f;
                                    int r = (int)(((color >> 16) & 0xff) * colorScale);
                                    int g = (int)(((color >> 8) & 0xff) * colorScale);
                                    int b = (int)(((color >> 0) & 0xff) * colorScale);
                                    color = (r << 16) + (g << 8) + b;
                                    */
                                    framebuffer.setRGB(x, y, color);
                                    // only update depth buffer when the pixel was drawn.
                                    depthBuffer[y * imageWidth + x] = z;
                                }
                            }
                            else {
                                depthBuffer[y * imageWidth + x] = z;
                                Color c = new Color(triangle.color);
                                float colorScale = 0.3f + nDotView * 0.7f;
                                int r = (int) (colorScale * c.getRed());
                                int g = (int) (colorScale * c.getGreen());
                                int b = (int) (colorScale * c.getBlue());
                                int rgb = (b << 16) + (g << 8) + r;
                                framebuffer.setRGB(x, y, rgb);
                            }
                        }
                    }
                }
            }

        }

        long t_duration = System.currentTimeMillis() - t_start;

        Graphics2D frameBufferGraphics = framebuffer.createGraphics();
        frameBufferGraphics.setColor(getForeground());
        frameBufferGraphics.drawString(""+t_duration+" ms, quads: "+drawnTriangles+"/"+mesh.tris.size()+" "+mesh.vertices.size()+" vertices", 50, 50);
        frameBufferGraphics.dispose();

        g2.drawImage(framebuffer, 0, 0, null);
    }
}
