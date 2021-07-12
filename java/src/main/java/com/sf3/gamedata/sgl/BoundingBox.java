package com.sf3.gamedata.sgl;

/** Axis oriented bounding box (AABB). Not a SGL class. */
public class BoundingBox {
    private Point min;
    private Point max;

    public BoundingBox() {
    }

    public BoundingBox(Point min, Point max) {
        this.min = min;
        this.max = max;
    }

    public void addPoint(Point p) {
        if (min == null) {
            min = p;
            max = p;
        }
        else {
            min = new Point(
                    new Fixed(Math.min(min.getX().getValue(), p.getX().getValue())),
                    new Fixed(Math.min(min.getY().getValue(), p.getY().getValue())),
                    new Fixed(Math.min(min.getZ().getValue(), p.getZ().getValue()))
            );
            max = new Point(
                    new Fixed(Math.max(max.getX().getValue(), p.getX().getValue())),
                    new Fixed(Math.max(max.getY().getValue(), p.getY().getValue())),
                    new Fixed(Math.max(max.getZ().getValue(), p.getZ().getValue()))
            );
        }
    }

    public Point getMax() {
        return max;
    }

    public Point getMin() {
        return min;
    }

    @Override
    public String toString() {
        return "[ " + min + ", " + max + " ]";
    }
}
