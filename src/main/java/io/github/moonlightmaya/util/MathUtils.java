package io.github.moonlightmaya.util;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector4d;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MathUtils {

    public static final Vector3d ZERO_VEC_3 = new Vector3d();

    private static final Map<String, Vector3f> FUN_COLORS = new HashMap<>() {{
        try {
            putAliased(this, parseColor("#FF02AD"), "fran", "francielly", "bunny");
            putAliased(this, parseColor("#A672EF"), "chloe", "space");
            putAliased(this, parseColor("#00F0FF"), "maya", "limits");
            putAliased(this, parseColor("#99BBEE"), "skye", "sky", "skylar");
            putAliased(this, parseColor("#FF2400"), "lily", "foxes", "fox");
            putAliased(this, parseColor("#FFECD2"), "kiri");
            putAliased(this, parseColor("#A155DA"), "luna", "moff", "moth");
        } catch (Exception impossible) {impossible.printStackTrace(); throw new IllegalStateException("impossible");}
    }};

    private static <K, V> void putAliased(Map<K, V> map, V v, K... ks) {
        for (K k : ks) map.put(k, v);
    }

    public static Vector3f parseColor(String color) throws IllegalArgumentException {
        if (color == null) return new Vector3f(1,1,1);
        if (color.startsWith("#")) {
            if (color.length() == 4) {
                int r = Integer.parseInt(color.substring(1,2), 16);
                int g = Integer.parseInt(color.substring(2,3), 16);
                int b = Integer.parseInt(color.substring(3,4), 16);
                return new Vector3f(r / 15f, g / 15f, b / 15f);
            }
            if (color.length() == 7) {
                int r = Integer.parseInt(color.substring(1,3), 16);
                int g = Integer.parseInt(color.substring(3,5), 16);
                int b = Integer.parseInt(color.substring(5,7), 16);
                return new Vector3f(r / 255f, g / 255f, b / 255f);
            }
            throw new IllegalArgumentException("Invalid hex string: expected #RGB or #RRGGBB");
        }
        for (String key : FUN_COLORS.keySet()) {
            if (key.equalsIgnoreCase(color)) return new Vector3f(FUN_COLORS.get(key));
        }
        return new Vector3f(1, 1, 1);
    }

    //Minecraft int colors are stored as ARGB in ints
    public static Vector4d intToRGBA(int color) {
        return new Vector4d(
                ((color >> 16) & 0xff) / 255.0,
                ((color >> 8) & 0xff) / 255.0,
                ((color >> 0) & 0xff) / 255.0,
                ((color >> 24) & 0xff) / 255.0
        );
    }

    public static int RGBAToInt(double r, double g, double b, double a) {
        return
                MathHelper.clamp((int) (r * 255), 0, 255) << 16 |
                MathHelper.clamp((int) (g * 255), 0, 255) << 8 |
                MathHelper.clamp((int) (b * 255), 0, 255) << 0 |
                MathHelper.clamp((int) (a * 255), 0, 255) << 24;
    }

    public static int RGBtoInt(Vector3d rgb) {
        return RGBAToInt(rgb.x, rgb.y, rgb.z, 0);
    }

    public static Vector3d fromVec3d(Vec3d mcVec) {
        return new Vector3d(mcVec.x, mcVec.y, mcVec.z);
    }

    public static BlockPos getBlockPos(Vector3d vec) {
        return getBlockPos(vec.x, vec.y, vec.z);
    }

    public static BlockPos getBlockPos(double x, double y, double z) {
        return new BlockPos((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
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
