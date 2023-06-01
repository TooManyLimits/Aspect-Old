package io.github.moonlightmaya.util;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;

import java.util.ArrayList;

public class MathUtils {

    //Useful constants
    public static final Vector3d ZERO_VEC_3 = new Vector3d();
    public static final Vector3f ZERO_VEC_3F = new Vector3f();

    public static Vector3d fromVec3d(Vec3d mcVec) {
        return new Vector3d(mcVec.x, mcVec.y, mcVec.z);
    }

    public static BlockPos getBlockPos(Vector3d vec) {
        return getBlockPos(vec.x, vec.y, vec.z);
    }

    public static BlockPos getBlockPos(double x, double y, double z) {
        return new BlockPos(
                MathHelper.floor(x),
                MathHelper.floor(y),
                MathHelper.floor(z)
        );
    }

    public static boolean epsilon(float v) {
        return Math.abs(v) < 0.0001;
    }

    public static float bezier(float p0, float p1, float p2, float p3, float t) {
        float d = 1 - t;
        float t2 = t * t;
        float d2 = d * d;
        return  d2 * (d * p0 + t * p1) +
                t2 * (d * p2 + t * p3);
    }

    /**
     * Literally just a stack of matrices that reuses memory
     */
    public static class ActualMatrixStack {
        private final ArrayList<Matrix4f> matrices = new ArrayList<>();
        int curIndex = 0; //index 1 above the top item
        int maxSize = 0; //the number of matrices that have been on the stack at its peak

        public void push(Matrix4f mat) {
            if (curIndex == maxSize) {
                matrices.add(new Matrix4f(mat));
                maxSize++;
            } else if (curIndex > maxSize) {
                throw new IllegalStateException("Current index should never be above max size - this is a bug in ActualMatrixStack!");
            } else {
                matrices.get(curIndex).set(mat);
            }
            curIndex++;
        }

        public void pop() {
            curIndex--;
            assert curIndex >= 0;
        }

        public void peekInto(Matrix4f dest) {
            dest.set(matrices.get(curIndex-1));
        }

        public boolean isEmpty() {
            return curIndex == 0;
        }
    }

}
