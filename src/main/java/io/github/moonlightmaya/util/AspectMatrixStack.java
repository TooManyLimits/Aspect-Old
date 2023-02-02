package io.github.moonlightmaya.util;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.ArrayList;

/**
 * A matrix stack that minimizes allocations.
 * This also applies transformations in an unusual (to me) order.
 * When calling translate(), rotate(), or scale(), it will act as though
 * it has performed that transformation *before* the transformation it
 * currently has in its stack. It will *post-multiply*. This is the same
 * behavior that JOML uses, as well as the vanilla matrix stack, so I'm
 * implementing this in the same way for convention.
 */
public class AspectMatrixStack {

    private final ArrayList<Matrix4f> positionMatrices = new ArrayList<>();
    private final ArrayList<Matrix3f> normalMatrices = new ArrayList<>();
    int curIndex; //index of the top item
    int maxSize; //the number of matrices that have been on the stack at its peak

    public AspectMatrixStack() {
        curIndex = 0;
        maxSize = 1;
        positionMatrices.add(new Matrix4f());
        normalMatrices.add(new Matrix3f());
    }

    public AspectMatrixStack translate(double x, double y, double z) {
        return translate((float) x, (float) y, (float) z);
    }

    public AspectMatrixStack translate(float x, float y, float z) {
        positionMatrices.get(curIndex).translate(x, y, z);
        return this;
    }

    public AspectMatrixStack scale(double x, double y, double z) {
        return scale((float) x, (float) y, (float) z);
    }

    public AspectMatrixStack scale(float x, float y, float z) {
        positionMatrices.get(curIndex).scale(x, y, z);
        if (x == y && y == z) {
            if (x > 0)
                return this; //If all positive, and uniform scaling, normals are not affected
            normalMatrices.get(curIndex).scale(-1);
        }
        float f = 1 / x;
        float g = 1 / y;
        float h = 1 / z;
        float i = MathHelper.fastInverseCbrt(f * g * h);
        normalMatrices.get(curIndex).scale(f * i, g * i, h * i);
        return this;
    }

    public AspectMatrixStack multiply(Quaternionf quaternion) {
        positionMatrices.get(curIndex).rotate(quaternion);
        normalMatrices.get(curIndex).rotate(quaternion);
        return this;
    }

    public AspectMatrixStack push() {
        curIndex++;
        if (curIndex == maxSize) {
            positionMatrices.add(new Matrix4f(positionMatrices.get(maxSize)));
            normalMatrices.add(new Matrix3f(normalMatrices.get(maxSize)));
            maxSize++;
        } else if (curIndex > maxSize) {
            throw new IllegalStateException("Current index should never be above max size - this is a bug in AspectMatrixStack!");
        } else {
            positionMatrices.get(curIndex).set(positionMatrices.get(curIndex-1));
            normalMatrices.get(curIndex).set(normalMatrices.get(curIndex-1));
        }
        return this;
    }

    public AspectMatrixStack pop() {
        curIndex--;
        return this;
    }

    public Matrix4f peekPosition()  {
        return positionMatrices.get(curIndex);
    }

    public Matrix3f peekNormal()  {
        return normalMatrices.get(curIndex);
    }

    public boolean isEmpty() {
        return curIndex == 0;
    }

    public AspectMatrixStack loadIdentity() {
        positionMatrices.get(curIndex).identity();
        normalMatrices.get(curIndex).identity();
        return this;
    }

    public AspectMatrixStack loadVanilla(MatrixStack vanillaStack) {
        MatrixStack.Entry peeked = vanillaStack.peek();
        positionMatrices.get(curIndex).set(peeked.getPositionMatrix());
        normalMatrices.get(curIndex).set(peeked.getNormalMatrix());
        return this;
    }

    public MatrixStack getVanillaCopy() {
        MatrixStack result = new MatrixStack();
        MatrixStack.Entry entry = result.peek();
        entry.getPositionMatrix().set(positionMatrices.get(curIndex));
        entry.getNormalMatrix().set(normalMatrices.get(curIndex));
        return result;
    }

}
