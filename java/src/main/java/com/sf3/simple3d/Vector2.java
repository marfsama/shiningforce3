package com.sf3.simple3d;

/** Vertex in 3 dimensional space. */
public class Vector2 {
    protected final float x;
    protected final float y;

    public Vector2() {
        this(0.0f, 0.0f);
    }

    public Vector2(float[] array) {
        this(array[0], array[1]);
    }

    public Vector2(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float length() {
        return (float) Math.sqrt(x*x + y*y);
    }

    /**
     * Normalise this vector
     */
    public Vector2 normalise() {
        float length = length();
        return scale(1.0f / length);
    }

    /**
     * Scale this vector.
     */
    public Vector2 scale(float factor) {
        return new Vector2(x * factor, y * factor);
    }

    /**
     * Dot product of this and other.
     */
    public float dotProduct(Vector2 other) {
        return x * other.x + y * other.y;
    }

    public Vector2 add(Vector2 other) {
        return add(other.x, other.y);
    }

    public Vector2 add(float x, float y) {
        return new Vector2(this.x + x, this.y + y);
    }

    public Vector2 sub(Vector2 other) {
        return sub(other.x, other.y);
    }

    public Vector2 sub(float x, float y) {
        return new Vector2(this.x - x, this.y - y);
    }

    @Override
    public String toString() {
        return "Vertex3{" +
                "x=" + x +
                ", y=" + y +
                '}';
    }
}
