package com.sf3.simple3d;


/** 3x3 Matrix Math. */
public class Matrix3 {
    protected final float[] values;

    private static final Matrix3 IDENTITY = new Matrix3(new float[]{
            1.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f,
            0.0f, 0.0f, 1.0f});


    public Matrix3(float[] values) {
        this.values = values;
    }

    public Matrix3 multiply(Matrix3 other) {
        float[] result = new float[9];
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                for (int i = 0; i < 3; i++) {
                    result[row * 3 + col] +=
                            this.values[row * 3 + i] * other.values[i * 3 + col];
                }
            }
        }
        return new Matrix3(result);
    }

    public Vector3 multiply(Vector3 in) {
        return new Vector3(
                in.x * values[0] + in.y * values[3] + in.z * values[6],
                in.x * values[1] + in.y * values[4] + in.z * values[7],
                in.x * values[2] + in.y * values[5] + in.z * values[8]
        );
    }


    public static final Matrix3 identity() {
        return IDENTITY;
    }

    private static final float cos(float phi) {
        return (float) Math.cos(phi);
    }

    private static final float sin(float phi) {
        return (float) Math.sin(phi);
    }

    /**
     * Matrix to rotate around the z axis. phi must be radians.
     */
    public static final Matrix3 rotateZ(float phi) {
        return new Matrix3(new float[]{
                cos(phi), -sin(phi), 0.0f,
                sin(phi), cos(phi), 0.0f,
                0.0f, 0.0f, 1.0f
        });
    }

    /**
     * Matrix to rotate around the y axis. phi must be radians.
     */
    public static final Matrix3 rotateY(float phi) {
        return new Matrix3(new float[]{
                cos(phi), 0.0f, -sin(phi),
                0.0f, 1.0f, 0.0f,
                sin(phi), 0.0f, cos(phi),
        });
    }

    /**
     * Matrix to rotate around the x axis. phi must be radians.
     */
    public static final Matrix3 rotateX(float phi) {
        return new Matrix3(new float[]{
                1.0f, 0.0f, 0.0f,
                0.0f, cos(phi), sin(phi),
                0.0f, -sin(phi), cos(phi),
        });
    }
}
