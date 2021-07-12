package com.sf3.vdp1;

import com.sf3.vdp1.model.ColorRam;

import javax.swing.*;
import java.awt.*;

/** shows the color ram. */
public class ColorRamPanel extends JPanel {

    private ColorRam colorRam;

    public ColorRam getColorRam() {
        return colorRam;
    }

    public void setColorRam(ColorRam colorRam) {
        this.colorRam = colorRam;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Rectangle rect = g.getClipBounds();
        g.clearRect(0,0, rect.width, rect.height);
        if (colorRam == null) {
            return;
        }
        int numColors = colorRam.getNumColors();
        int tileWith = 25;
        int width = 64;
        int height = numColors / width;

        for (int i = 0; i < numColors; i++) {
            int x = i % width;
            int y = i / width;
            g.setColor(new Color(colorRam.getColor(0, i)));
            g.fillRect(x*tileWith,y*tileWith,tileWith,tileWith);
        }

    }
}
