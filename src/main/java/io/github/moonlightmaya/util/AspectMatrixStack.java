package io.github.moonlightmaya.util;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import org.joml.*;

import java.lang.Math;
import java.util.ArrayList;

/**
 * A matrix stack that doesn't allocate when popping then pushing.
 * When calling translate(), rotate(), or scale(), it will act as though
 * it has performed that transformation *before* the transformation it
 * currently has in its stack. It will *post-multiply*. This is the same
 * behavior that JOML uses, as well as the vanilla matrix stack, so I'm
 * implementing this in the same way for convention.
 */
public class AspectMatrixStack {

    private final ArrayList<Matrix4d> positionMatrices = new ArrayList<>();
    private final ArrayList<Matrix3d> normalMatrices = new ArrayList<>();
    int curIndex; //index of the top item
    int maxSize; //the number of matrices that have been on the stack at its peak

    public AspectMatrixStack() {
        curIndex = 0;
        maxSize = 1;
        positionMatrices.add(new Matrix4d());
        normalMatrices.add(new Matrix3d());
    }

    public AspectMatrixStack(MatrixStack vanillaStack) {
        this();
        MatrixStack.Entry peeked = vanillaStack.peek();
        positionMatrices.get(curIndex).set(peeked.getPositionMatrix());
        normalMatrices.get(curIndex).set(peeked.getNormalMatrix());
    }

    public void translate(Vector3dc vec) {
        translate(vec.x(), vec.y(), vec.z());
    }

    public void translate(Vector3fc vec) {
        translate(vec.x(), vec.y(), vec.z());
    }

    public void translate(double x, double y, double z) {
        positionMatrices.get(curIndex).translate(x, y, z);
    }

    public void scale(Vector3dc vec) {
        scale(vec.x(), vec.y(), vec.z());
    }

    public void scale(Vector3fc vec) {
        scale(vec.x(), vec.y(), vec.z());
    }

    public void scale(double x, double y, double z) {
        positionMatrices.get(curIndex).scale(x, y, z);
        if (x == y && y == z) {
            if (x > 0)
                return; //If all positive, and uniform scaling, normals are not affected
            normalMatrices.get(curIndex).scale(-1);
        }
        double f = 1 / x;
        double g = 1 / y;
        double h = 1 / z;
        double i = 1 / Math.cbrt(f * g * h);
        normalMatrices.get(curIndex).scale(f * i, g * i, h * i);
    }

    public void rotate(Quaternionf quaternion) {
        positionMatrices.get(curIndex).rotate(quaternion);
        normalMatrices.get(curIndex).rotate(quaternion);
    }

    public void multiply(Matrix4f posMatrix, Matrix3f normalMatrix) {
        positionMatrices.get(curIndex).mul(posMatrix);
        normalMatrices.get(curIndex).mul(normalMatrix);
    }

    public void multiply(Matrix4d posMatrix, Matrix3d normalMatrix) {
        positionMatrices.get(curIndex).mul(posMatrix);
        normalMatrices.get(curIndex).mul(normalMatrix);
    }

    private static final Matrix3f normal = new Matrix3f();
    public void multiply(Matrix4f posMatrix) {
        positionMatrices.get(curIndex).mul(posMatrix);
        posMatrix.normal(normal);
        normalMatrices.get(curIndex).mul(normal);
    }
    private static final Matrix3d normalD = new Matrix3d();
    public void multiply(Matrix4d posMatrix) {
        positionMatrices.get(curIndex).mul(posMatrix);
        posMatrix.normal(normalD);
        normalMatrices.get(curIndex).mul(normalD);
    }

    public void push() {
        curIndex++;
        if (curIndex == maxSize) {
            positionMatrices.add(new Matrix4d(positionMatrices.get(curIndex-1)));
            normalMatrices.add(new Matrix3d(normalMatrices.get(curIndex-1)));
            maxSize++;
        } else if (curIndex > maxSize) {
            throw new IllegalStateException("Current index should never be above max size - this is a bug in AspectMatrixStack!");
        } else {
            positionMatrices.get(curIndex).set(positionMatrices.get(curIndex-1));
            normalMatrices.get(curIndex).set(normalMatrices.get(curIndex-1));
        }
    }

    public void pop() {
        curIndex--;
    }

    public Matrix4d peekPosition()  {
        return positionMatrices.get(curIndex);
    }

    public Matrix3d peekNormal()  {
        return normalMatrices.get(curIndex);
    }

    public boolean isEmpty() {
        return curIndex == 0;
    }

    public void loadIdentity() {
        positionMatrices.get(curIndex).identity();
        normalMatrices.get(curIndex).identity();
    }

    /**
     * Converts the matrix stack to a vanilla matrix stack. Note that PRECISION
     * MAY BE LOST if doing this with large numbers, since vanilla matrix stacks
     * use floats, while this class uses doubles! Ensure that large numbers are
     * not in the top of the stack when calling this!
     */
    public MatrixStack getVanillaCopy() {
        MatrixStack result = new MatrixStack();
        MatrixStack.Entry entry = result.peek();
        entry.getPositionMatrix().set(positionMatrices.get(curIndex));
        //JOML doesnt have a method like the above for Matrix3f, so I made our own helper
        set(entry.getNormalMatrix(), normalMatrices.get(curIndex));
        return result;
    }

    //I can't do "entry.getNormalMatrix().set(normalMatrices.get(curIndex));" since JOML never added that method,
    //so I made this instead
    private final float[] buffer = new float[9];
    private void set(Matrix3f toEdit, Matrix3dc toCopy) {
        toEdit.set(toCopy.get(buffer));
    }

}
