package com.sf3.simple3d;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class OrbitingCamera extends TimerTask implements Camera {
    private List<RenderPanel> renderPanels = new ArrayList<>();
    private Timer timer;
    private Matrix4 worldToCamera = Matrix4.identity();
    private float angle = 0;
    private float speed;
    private float distance = 40.400412f;
    private float pitch = 25;

    public OrbitingCamera(float speed) {
        this.speed = speed;
        this.timer = new Timer(true);
        timer.scheduleAtFixedRate(this, 0, 1000 / 50);
    }

    public void setDistance(float distance) {
        this.distance = distance;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public void addRenderPanel(RenderPanel renderPanel) {
        this.renderPanels.add(renderPanel);
    }

    @Override
    public void run() {
        angle += speed;
        if (angle > 180) {
            angle -= 360;
        }

        Matrix4 translate = Matrix4.translate(0, 1000, -distance);
        Matrix4 rotateX = Matrix4.rotateX((float) Math.toRadians(pitch));
        Matrix4 rotateY = Matrix4.rotateY((float) Math.toRadians(angle));

        this.worldToCamera = translate.multiply(rotateX);


        renderPanels.stream().forEach(
                (panel) -> SwingUtilities.invokeLater(() -> panel.repaint())
        );
    }

    @Override
    public Matrix4 getTransform() {
        return worldToCamera;
    }
}
