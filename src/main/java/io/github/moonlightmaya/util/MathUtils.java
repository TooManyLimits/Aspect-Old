package io.github.moonlightmaya.util;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector4d;

import java.util.ArrayList;

public class MathUtils {

    public static final Vector3d ZERO_VEC_3 = new Vector3d();

    //Minecraft int colors are stored as ARGB in ints
    public static Vector4d intToRGBA(int color) {
        return new Vector4d(
                ((color >> 16) & 0xff) / 255.0,
                ((color >> 8) & 0xff) / 255.0,
                ((color >> 0) & 0xff) / 255.0,
                ((color >> 24) & 0xff) / 255.0
        );
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
