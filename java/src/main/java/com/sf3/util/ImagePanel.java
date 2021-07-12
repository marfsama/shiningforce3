package com.sf3.util;

import com.jgoodies.binding.value.ValueModel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/** Draws a scaled image. */
public class ImagePanel extends JPanel implements PropertyChangeListener {
    private final ValueModel imageModel;
    private final ValueModel scaleModel;

    public ImagePanel(ValueModel imageModel, ValueModel scaleModel, ValueModel panelSizeModel, ValueModel mousePosModel) {
        this.imageModel = imageModel;
        this.scaleModel = scaleModel;

        imageModel.addValueChangeListener(this);
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                panelSizeModel.setValue(e.getComponent().getSize());
            }
        });
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Point p = e.getPoint();
                int scale = (int) scaleModel.getValue();
                mousePosModel.setValue(new Point(p.x / scale, p.y / scale));
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        BufferedImage image = imageModel == null ? null : (BufferedImage) imageModel.getValue();
        Integer scale = scaleModel == null ? null : (Integer) scaleModel.getValue();
        if (image != null && scale != null && scale > 0) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setTransform(AffineTransform.getScaleInstance(scale, scale));
            g2d.drawImage(image, 0, 0, this);
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        repaint();
    }
}
