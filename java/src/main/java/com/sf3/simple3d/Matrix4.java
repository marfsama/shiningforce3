package com.sf3.simple3d;

public class Matrix4 {
    protected final float[][] values;

    private static final Matrix4 IDENTITY = new Matrix4(new float[][]{
            {1.0f, 0.0f, 0.0f, 0.0f},
            {0.0f, 1.0f, 0.0f, 0.0f},
            {0.0f, 0.0f, 1.0f, 0.0f},
            {0.0f, 0.0f, 0.0f, 1.0f},
    });

    public Matrix4(float[][] values) {
        this.values = values;
    }

    public Matrix4 multiply(Matrix4 other) {
        float[][] result = new float[4][4];
        float[][] a = this.values;
        float[][] b = other.values;
        for (int i = 0; i < 4; ++i) {
            for (int  j = 0; j < 4; ++j) {
                result[i][j] = a[i][0] * b[0][j] + a[i][1] * b[1][j] +
                        a[i][2] * b[2][j] + a[i][3] * b[3][j];
            }
        }
        return new Matrix4(result);
    }

    public Matrix4 transpose() {
        float[][] transposed = new float[4][4];
        for (int i = 0; i < 4; ++i) {
            for (int j = 0; j < 4; ++j) {
                transposed[i][j] = values[j][i];
            }
        }
        return new Matrix4(transposed);
    }

    /** Used for point multiplication. the components are divided by w (which is original z). */
    public Vector3 multiplyVector(Vector3 vec) {
        float[] src = new float[]{vec.x, vec.y, vec.z};
        float[][] m = values;
        float a = vec.x * m[0][0] + vec.y * m[1][0] + vec.z * m[2][0] + /* vec.w = 1 */m[3][0];
        float b = vec.x * m[0][1] + vec.y * m[1][1] + vec.z * m[2][1] + /* vec.w = 1 */m[3][1];
        float c = vec.x * m[0][2] + vec.y * m[1][2] + vec.z * m[2][2] + /* vec.w = 1 */m[3][2];
        float w = vec.x * m[0][3] + vec.y * m[1][3] + vec.z * m[2][3] + /* vec.w = 1 */m[3][3];

        if (w != 1.0f && w != 0.0f) {
            float x = a / w;
            float y = b / w;
            float z = c / w;
            return new Vector3(x,y,z);
        }
        return new Vector3(a,b,c);
    }

    /** Used for vector multiplication. The components for translation are not used and w is not computed. */
    public Vector3 multiplyDirection(Vector3 vec) {
        float[] src = new float[]{vec.x, vec.y, vec.z};
        float[][] m = values;
        float a = vec.x * m[0][0] + vec.y * m[1][0] + vec.z * m[2][0];
        float b = vec.x * m[0][1] + vec.y * m[1][1] + vec.z * m[2][1];
        float c = vec.x * m[0][2] + vec.y * m[1][2] + vec.z * m[2][2];

        return new Vector3(a,b,c);
    }

    private float[][] deepCopy(float[][] matrix) {
        return java.util.Arrays.stream(matrix).map(el -> el.clone()).toArray($ -> matrix.clone());
    }

    public Matrix4 inverse() {
        int i, j, k;
        float[][] s = new float[4][4];
        float[][] t = deepCopy(values);

        // Forward elimination
        for (i = 0; i < 3 ; i++) {
            int pivot = i;

            float pivotsize = t[i][i];

            if (pivotsize < 0)
                pivotsize = -pivotsize;

            for (j = i + 1; j < 4; j++) {
                float tmp = t[j][i];

                if (tmp < 0)
                    tmp = -tmp;

                if (tmp > pivotsize) {
                    pivot = j;
                    pivotsize = tmp;
                }
            }

            if (pivotsize == 0) {
                // Cannot invert singular matrix
                return null;
            }

            if (pivot != i) {
                for (j = 0; j < 4; j++) {
                    float tmp;

                    tmp = t[i][j];
                    t[i][j] = t[pivot][j];
                    t[pivot][j] = tmp;

                    tmp = s[i][j];
                    s[i][j] = s[pivot][j];
                    s[pivot][j] = tmp;
                }
            }

            for (j = i + 1; j < 4; j++) {
                float f = t[j][i] / t[i][i];

                for (k = 0; k < 4; k++) {
                    t[j][k] -= f * t[i][k];
                    s[j][k] -= f * s[i][k];
                }
            }
        }

        // Backward substitution
        for (i = 3; i >= 0; --i) {
            float f;

            if ((f = t[i][i]) == 0) {
                // Cannot invert singular matrix
                return null;
            }

            for (j = 0; j < 4; j++) {
                t[i][j] /= f;
                s[i][j] /= f;
            }

            for (j = 0; j < i; j++) {
                f = t[j][i];

                for (k = 0; k < 4; k++) {
                    t[j][k] -= f * t[i][k];
                    s[j][k] -= f * s[i][k];
                }
            }
        }

        return new Matrix4(s);
    }

    public static Matrix4 identity() {
        return IDENTITY;
    }

    private static final float cos(float phi) {
        return (float) Math.cos(phi);
    }

    private static final float sin(float phi) {
        return (float) Math.sin(phi);
    }
    private static final float tan(double phi) {
        return (float) Math.tan(phi);
    }

    public static final Matrix4 rotateX(float phiX) {
        return new Matrix4(new float[][] {
                {1.0f, 0.0f, 0.0f, 0.0f},
                {0.0f, cos(phiX), sin(phiX), 0.0f},
                {0.0f, -sin(phiX), cos(phiX), 0.0f},
                {0.0f, 0.0f, 0.0f, 1.0f}
        });
    }

    public static final Matrix4 rotateY(float phi) {
        return new Matrix4(new float[][] {
            {cos(phi), 0.0f, -sin(phi), 0.0f},
            {0.0f, 1.0f, 0.0f, 0.0f},
            {sin(phi), 0.0f, cos(phi), 0.0f},
            {0.0f, 0.0f, 0.0f, 1.0f}
        });
    }

    public static final Matrix4 rotateZ(float phi) {
        return new Matrix4(new float[][] {
                {cos(phi), -sin(phi), 0.0f, 0.0f},
                {sin(phi), cos(phi), 0.0f, 0.0f},
                {0.0f, 0.0f, 1.0f, 0.0f},
                {0.0f, 0.0f, 0.0f, 1.0f}
        });
    }

    public static final Matrix4 rotate(float phiX, float phiY, float phiZ) {
        return new Matrix4(new float[][] {
                {1.0f, 0.0f, 0.0f, 0.0f},
                {0.0f, cos(phiX), sin(phiX), 0.0f},
                {0.0f, -sin(phiX), cos(phiX), 0.0f},
                {0.0f, 0.0f, 0.0f, 1.0f}
        });
    }

    public static Matrix4 translate(float x, float y, float z) {
        return new Matrix4(new float[][] {
                {1.0f, 0.0f, 0.0f, 0.0f},
                {0.0f, 1.0f, 0.0f, 0.0f},
                {0.0f, 0.0f, 1.0f, 0.0f},
                {x, y, z, 1.0f},
        });
    }

    public static Matrix4 mirrorX() {
        return new Matrix4(new float[][] {
                {-1.0f, 0.0f, 0.0f, 0.0f},
                { 0.0f, 1.0f, 0.0f, 0.0f},
                { 0.0f, 0.0f, 1.0f, 0.0f},
                { 0.0f, 0.0f, 0,0f, 1.0f},
        });
    }

    public static Matrix4 mirrorY() {
        return new Matrix4(new float[][] {
                { 1.0f, 0.0f, 0.0f, 0.0f},
                { 0.0f,-1.0f, 0.0f, 0.0f},
                { 0.0f, 0.0f, 1.0f, 0.0f},
                { 0.0f, 0.0f, 0,0f, 1.0f},
        });
    }

    public static Matrix4 mirrorZ() {
        return new Matrix4(new float[][] {
                { 1.0f, 0.0f, 0.0f, 0.0f},
                { 0.0f, 1.0f, 0.0f, 0.0f},
                { 0.0f, 0.0f,-1.0f, 0.0f},
                { 0.0f, 0.0f, 0,0f, 1.0f},
        });
    }

    /**
     * Creates a perspective matrix.
     *
     * @param near near clipping plane
     * @param far far clipping plane
     * @param angleOfView horizontal angle of view
     * @param aspectRatio aspect ratio (imageWidth / (float)imageHeight)
     * @see "https://www.scratchapixel.com/lessons/3d-basic-rendering/perspective-and-orthographic-projection-matrix/opengl-perspective-projection-matrix"
     */
    public static  final Matrix4 perspective(float near, float far, float angleOfView, float aspectRatio) {
        float[][] values = new float[4][4];
        // set the basic projection matrix
        float scale = tan(angleOfView * 0.5 * Math.PI / 180) * near;
        float right = aspectRatio * scale;
        float left = -right;
        float top = scale;
        float bottom = -top;


        values[0][0] = 2 * near / (right - left); // scale the x coordinates of the projected point
        values[1][1] = 2 * near / (top - bottom); // scale the y coordinates of the projected point

        values[2][0] = (right + left) / (right - left); // remap x to [-1,1]
        values[2][1] = (top + bottom) / (top - bottom); // remap y to [-1,1]
        values[2][2] = -(far + near) / (far - near);  // A: used to remap z [-1,1]
        values[2][3] = -1; // remap -z to w

        values[3][2] = -2 * far * near / (far - near); // B: used to remap z [-1,1]
        values[3][3] = 0;

        return new Matrix4(values);
    }

    /**
     * Creates a perspective matrix.
     *
     * @param near near clipping plane
     * @param far far clipping plane
     * @param left left frustum plane
     * @param right right frustum plane
     * @param top top frustum plane
     * @param bottom bottom frustum plane
     * @return the perspective matrix
     * @see "https://www.scratchapixel.com/lessons/3d-basic-rendering/perspective-and-orthographic-projection-matrix/opengl-perspective-projection-matrix"
     */
    public static  final Matrix4 perspective(float near, float far, float left, float right, float top, float bottom) {
        float[][] values = new float[4][4];
        // set the basic projection matrix

        values[0][0] = 2 * near / (right - left); // scale the x coordinates of the projected point
        values[1][1] = 2 * near / (top - bottom); // scale the y coordinates of the projected point

        values[2][0] = (right + left) / (right - left); // remap x to [-1,1]
        values[2][1] = (top + bottom) / (top - bottom); // remap y to [-1,1]
        values[2][2] = -(far + near) / (far - near);  // A: used to remap z [-1,1]
        values[2][3] = -1; // remap -z to w

        values[3][2] = -2 * far * near / (far - near); // B: used to remap z [-1,1]
        values[3][3] = 0;

        return new Matrix4(values);
    }


}
