package com.sf3.simple3d;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Triangle {
    protected int v1;
    protected int v2;
    protected int v3;

    protected int t1 = -1;
    protected int t2 = -1;
    protected int t3 = -1;

    protected BufferedImage texture;
    protected int color = Color.WHITE.getRGB();

    public Triangle(int v1, int v2, int v3) {
        this.v1 = v1;
        this.v2 = v2;
        this.v3 = v3;
    }

    public Triangle(int v1, int v2, int v3, int color) {
        this.v1 = v1;
        this.v2 = v2;
        this.v3 = v3;
        this.color = color;
    }

    public Triangle(int v1, int v2, int v3, int t1, int t2, int t3, BufferedImage texture) {
        this.v1 = v1;
        this.v2 = v2;
        this.v3 = v3;
        this.t1 = t1;
        this.t2 = t2;
        this.t3 = t3;
        this.texture = texture;
    }

    public boolean hasUV() {
        return t1 >= 0 && t2 >= 0 && t3 >= 0 && texture != null;
    }

}
