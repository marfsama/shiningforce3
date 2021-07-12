package com.sf3.simple3d;

import java.util.Objects;

/** Vertex in 3 dimensional space. */
public class Vector3 {
    protected final float x;
    protected final float y;
    protected final float z;

    public Vector3() {
        this(0.0f, 0.0f, 0.0f);
    }

    public Vector3(float[] array) {
        this(array[0], array[1], array[2]);
    }


    public Vector3(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getZ() {
        return z;
    }

    public float length() {
        return (float) Math.sqrt(x*x + y*y + z*z);
    }

    /**
     * Normalise this vector
     */
    public Vector3 normalize() {
        float length = length();
        return scale(1.0f / length);
    }

    /**
     * Scale this vector.
     */
    public Vector3 scale(float factor) {
        return new Vector3(x * factor, y * factor, z * factor);
    }

    /**
     * Cross product of this and other.
     */
    public Vector3 crossProduct(Vector3 other) {
        return  new Vector3(
        (y * other.z) - (z * other.y),
        (z * other.x) - (x * other.z),
        (x * other.y) - (y * other.x));
    }

    /**
     * Dot product of this and other.
     */
    public float dotProduct(Vector3 other) {
        return x * other.x + y * other.y + z * other.z;
    }

    public Vector3 add(Vector3 other) {
        return add(other.x, other.y, other.z);
    }

    public Vector3 add(float x, float y, float z) {
        return new Vector3(this.x + x, this.y + y, this.z + z);
    }

    public Vector3 sub(Vector3 other) {
        return sub(other.x, other.y, other.z);
    }

    public Vector3 sub(float x, float y, float z) {
        return new Vector3(this.x - x, this.y - y, this.z - z);
    }

    public Vector3 negate() {
        return new Vector3(-x, -y, -z);
    }

    public Vector3 x(float x) {
        return new Vector3(x, this.y, this.z);
    }

    public Vector3 y(float y) {
        return new Vector3(this.x, y, this.z);
    }

    public Vector3 z(float z) {
        return new Vector3(this.x, this.y, z);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Vector3)) return false;
        Vector3 vector3 = (Vector3) o;
        return Float.compare(vector3.x, x) == 0 &&
                Float.compare(vector3.y, y) == 0 &&
                Float.compare(vector3.z, z) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z);
    }

    @Override
    public String toString() {
        return "Vertex3{" +
                "x=" + x +
                ", y=" + y +
                ", z=" + z +
                '}';
    }
}
