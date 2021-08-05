package com.sf3.util;

import com.jgoodies.binding.value.ValueModel;
import com.sf3.binaryview.HighlightGroup;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

/** Draws a scaled image. */
public class ImagePanel extends JPanel implements PropertyChangeListener {
    private final ValueModel imageModel;
    private final ValueModel scaleModel;
    private final ValueModel textureWidthModel;
    private final ValueModel fileOffset;
    private final ValueModel highlightGroups;

    public ImagePanel(ValueModel imageModel, ValueModel scaleModel, ValueModel panelSizeModel, ValueModel mousePosModel, ValueModel textureWidthModel, ValueModel fileOffset) {
        this(imageModel, scaleModel, panelSizeModel, mousePosModel, textureWidthModel, fileOffset, null);
    }

    public ImagePanel(ValueModel imageModel, ValueModel scaleModel, ValueModel panelSizeModel, ValueModel mousePosModel, ValueModel textureWidthModel, ValueModel fileOffset, ValueModel highlightGroups) {
        this.imageModel = imageModel;
        this.scaleModel = scaleModel;
        this.textureWidthModel = textureWidthModel;
        this.fileOffset = fileOffset;
        this.highlightGroups = highlightGroups;

        imageModel.addValueChangeListener(this);
        if (highlightGroups != null) {
            highlightGroups.addValueChangeListener(this);
        }

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
        Rectangle clipBounds = g.getClipBounds();
        int offset = fileOffset != null && fileOffset.getValue() != null ? (Integer) fileOffset.getValue() : 0;
        if (image != null && scale != null && scale > 0) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setTransform(AffineTransform.getScaleInstance(scale, scale));
            g2d.drawImage(image, 0, 0, this);
            drawColumns((Graphics2D) g2d.create(), image.getWidth(), image.getHeight(), scale);
            if (highlightGroups != null && highlightGroups.getValue() != null) {
                List<HighlightGroup> groups = (List<HighlightGroup>) highlightGroups.getValue();
                for (HighlightGroup group : groups) {
                    Color color = new Color(group.getColor());
                    group.getHighlights().forEach(
                            highlight -> drawHighlight((Graphics2D) g2d.create(), image.getWidth(), image.getHeight(), scale, highlight.getStart() - offset, highlight.getEnd() - offset, color)
                    );

                    group.getPointers().forEach(
                            pointer -> drawArrow((Graphics2D) g2d.create(), image.getWidth(), image.getHeight(), scale, pointer.getStart() - offset, pointer.getDestination() - offset, color)
                    );

                }
            }
        }
    }

    /** Draws an arrow from one point of the file to another. */
    private void drawArrow(Graphics2D g2d, int width, int height, Integer scale, int start, int destination, Color color) {
        if (textureWidthModel != null && textureWidthModel.getValue() != null) {
            int textureWidth = (Integer) textureWidthModel.getValue();
            int columnSize = textureWidth * height;

            int x1 = ((start / 2) / columnSize) * textureWidth + (start / 2) % textureWidth;
            int y1 = ((start / 2) % columnSize) / textureWidth;

            int x2 = ((destination / 2) / columnSize) * textureWidth + (destination / 2) % textureWidth;
            int y2 = ((destination / 2) % columnSize) / textureWidth;

            double angle = Math.atan2(y2-y1, x2-x1);

            g2d.setTransform(AffineTransform.getScaleInstance(1.0, 1.0));
            g2d.setStroke(new BasicStroke(3));
            g2d.setColor(color);
            g2d.fillArc(x2*scale,y2*scale, scale, scale, 0, 360);
            g2d.translate(scale/2, scale/2);
            g2d.drawLine(x1*scale,y1*scale,x2*scale,y2*scale);
        }
    }

    /** Highlights a portion of the file. */
    private void drawHighlight(Graphics2D g2d, int width, int height, Integer scale, int start, int end, Color color) {
        if (textureWidthModel != null && textureWidthModel.getValue() != null) {
            int textureWidth = (Integer) textureWidthModel.getValue();
            int columnSize = textureWidth * height;

            g2d.setTransform(AffineTransform.getScaleInstance(1.0, 1.0));
            g2d.setColor(color);
            // find pixel in the column at which the highlight starts
            int firstRowStart = (start / 2) % textureWidth;
            int firstRow = (start / 2) / textureWidth;
            int lastRowEnd = (end / 2) % textureWidth;
            int lastRow = (end / 2) / textureWidth;
            boolean oneRow = firstRow == lastRow;
            if (firstRowStart > 0 || oneRow) {
                firstRow++;
                // the first row starts *not* at the first row. draw the first part of the highlight
                int column = (start / 2) / columnSize;
                int row = ((start / 2) % columnSize) / textureWidth;

                int x = column * textureWidth + firstRowStart;
                int w = oneRow ? lastRowEnd - firstRowStart :  textureWidth - firstRowStart;
                int y = row;
                int h = 1;

                g2d.setComposite(AlphaComposite.SrcOver.derive(0.5f));
                g2d.fillRect(x * scale, y * scale, w * scale, h * scale);
                g2d.setComposite(AlphaComposite.SrcOver);
                g2d.drawRect(x * scale, y * scale, w * scale, h * scale);
            }
            if (lastRowEnd > 0 && !oneRow) {
                // the first row starts *not* at the first row. draw the first part of the highlight
                int column = (end / 2) / columnSize;
                int row = ((end / 2) % columnSize) / textureWidth;

                int x = column * textureWidth;
                int w = lastRowEnd;
                int y = row;
                int h = 1;
                g2d.setComposite(AlphaComposite.SrcOver.derive(0.5f));
                g2d.fillRect(x * scale, y * scale, w * scale, h * scale);
                g2d.setComposite(AlphaComposite.SrcOver);
                g2d.drawRect(x * scale, y * scale, w * scale, h * scale);
            }
            if (lastRow - firstRow >= 1) {
                int row = firstRow;
                do {
                    // the first row starts *not* at the first row. draw the first part of the highlight
                    int column = row / height;

                    int x = column * textureWidth;
                    int w = textureWidth;
                    int y = row % height;
                    int h = 1;
                    g2d.setComposite(AlphaComposite.SrcOver.derive(0.5f));
                    g2d.fillRect(x * scale, y * scale, w * scale, h * scale);
                    g2d.setComposite(AlphaComposite.SrcOver);
                    g2d.drawRect(x * scale, y * scale, w * scale, h * scale);
                    row++;
                } while (row < lastRow);
            }

        }
    }


    private void drawColumns(Graphics2D g2d, int width, int height, Integer scale) {
        if (textureWidthModel != null && textureWidthModel.getValue() != null) {
            int scaledTextureWidth = ((Integer) textureWidthModel.getValue()) * scale;
            g2d.setTransform(AffineTransform.getScaleInstance(1.0, 1.0));
            for (int i = scaledTextureWidth; i < width * scale; i += scaledTextureWidth) {
                g2d.setColor(Color.BLACK);
                g2d.drawLine(i, 0, i, height * scale);
                g2d.setColor(Color.WHITE);
                g2d.drawLine(i+1, 0, i+1, height * scale);
            }
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        repaint();
    }
}
