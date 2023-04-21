package io.github.moonlightmaya.script.apis.math;

import org.joml.*;
import petpet.lang.run.JavaFunction;
import petpet.lang.run.PetPetClass;
import petpet.lang.run.PetPetException;

public class Matrices {

    public static final PetPetClass MAT_2, MAT_3, MAT_4;
    public static final JavaFunction MAT_2_CREATE, MAT_3_CREATE, MAT_4_CREATE;


    private static void registerHelper(String petpetName, String javaName, Class<?>... args) {
        MAT_2.addMethod(petpetName, new JavaFunction(Matrix2d.class, javaName, true, args));
        MAT_3.addMethod(petpetName, new JavaFunction(Matrix3d.class, javaName, true, args));
        MAT_4.addMethod(petpetName, new JavaFunction(Matrix4d.class, javaName, true, args));
    }

    private static void registerHelper(String petpetName, String javaName) {
        MAT_2.addMethod(petpetName, new JavaFunction(Matrix2d.class, javaName, true));
        MAT_3.addMethod(petpetName, new JavaFunction(Matrix3d.class, javaName, true));
        MAT_4.addMethod(petpetName, new JavaFunction(Matrix4d.class, javaName, true));
    }

    //Note how the 4d version also only has 3 doubles, this is because 4d matrices in graphics generally
    //are used to deal with 3d structures.
    private static void registerHelperDType(String petpetName, String javaName) {
        MAT_2.addMethod(petpetName + "_2", new JavaFunction(Matrix2d.class, javaName, true, double.class, double.class));
        MAT_3.addMethod(petpetName + "_3", new JavaFunction(Matrix3d.class, javaName, true, double.class, double.class, double.class));
        MAT_4.addMethod(petpetName + "_3", new JavaFunction(Matrix4d.class, javaName, true, double.class, double.class, double.class));
    }

    private static void registerHelperCType(String petpetName, String javaName) {
        MAT_2.addMethod(petpetName, new JavaFunction(Matrix2d.class, javaName, true, Matrix2dc.class));
        MAT_3.addMethod(petpetName, new JavaFunction(Matrix3d.class, javaName, true, Matrix3dc.class));
        MAT_4.addMethod(petpetName, new JavaFunction(Matrix4d.class, javaName, true, Matrix4dc.class));
    }

    private static void registerHelperOverload(String petpetName, String type, String javaNameWithoutNumber) {
        MAT_2.addMethod(petpetName + "_" + type + (type.equals("num") ? "" : "2"), new JavaFunction(Matrices.class, javaNameWithoutNumber + "2", false));
        MAT_3.addMethod(petpetName + "_" + type + (type.equals("num") ? "" : "3"), new JavaFunction(Matrices.class, javaNameWithoutNumber + "3", false));
        MAT_4.addMethod(petpetName + "_" + type + (type.equals("num") ? "" : "4"), new JavaFunction(Matrices.class, javaNameWithoutNumber + "4", false));
    }

    static {
        try {
            MAT_2 = new PetPetClass("mat2");
            MAT_3 = new PetPetClass("mat3");
            MAT_4 = new PetPetClass("mat4");

            MAT_2_CREATE = new JavaFunction(Matrices.class, "create2", false);
            MAT_3_CREATE = new JavaFunction(Matrices.class, "create3", false);
            MAT_4_CREATE = new JavaFunction(Matrices.class, "create4", false);

            //Common
            registerHelper("det", "determinant");
            registerHelper("inv", "invert", new Class<?>[0]);
            registerHelper("get", "get", int.class, int.class);
            registerHelper("set_3", "set", int.class, int.class, double.class);
            registerHelper("reset", "identity");
            registerHelper("transpose", "transpose", new Class<?>[0]);

            registerHelperCType("set_1", "set");

            //Copy and getting by string
            registerHelperOverload("__get", "str", "getStr");
            //Get columns
            registerHelperOverload("__get", "num", "getNum");


            //In place modification
            registerHelperCType("add", "add");
            registerHelperCType("sub", "add");
            registerHelperCType("mul", "mul");
            registerHelperCType("mulR", "mulLocal");

            //Scale
            MAT_2.addMethod("scale_1", new JavaFunction(Matrices.class, "scale2", false));
            MAT_3.addMethod("scale_1", new JavaFunction(Matrices.class, "scale3", false));
            MAT_4.addMethod("scale_1", new JavaFunction(Matrices.class, "scale4", false));
            registerHelperDType("scale", "scale");

            //Rotate
            MAT_2.addMethod("rot", new JavaFunction(Matrix2d.class, "rotate", true, double.class));

            MAT_3.addMethod("rotXYZ_1", new JavaFunction(Matrices.class, "rotXYZ3", false));
            MAT_3.addMethod("rotXYZ_3", new JavaFunction(Matrix3d.class, "rotateXYZ", true, double.class, double.class, double.class));
            MAT_3.addMethod("rotZYX_1", new JavaFunction(Matrices.class, "rotZYX3", false));
            MAT_3.addMethod("rotZYX_3", new JavaFunction(Matrix3d.class, "rotateZYX", true, double.class, double.class, double.class));

            MAT_4.addMethod("rotXYZ_1", new JavaFunction(Matrix4d.class, "rotateXYZ", true, Vector3d.class));
            MAT_4.addMethod("rotXYZ_3", new JavaFunction(Matrix4d.class, "rotateXYZ", true, double.class, double.class, double.class));
            MAT_4.addMethod("rotZYX_1", new JavaFunction(Matrix4d.class, "rotateZYX", true, Vector3d.class));
            MAT_4.addMethod("rotZYX_3", new JavaFunction(Matrix4d.class, "rotateZYX", true, double.class, double.class, double.class));

            //Translate
            MAT_4.addMethod("pos_1", new JavaFunction(Matrix4d.class, "translate", true, Vector3dc.class));
            MAT_4.addMethod("pos_3", new JavaFunction(Matrix4d.class, "translate", true, double.class, double.class, double.class));
            MAT_4.addMethod("posR_1", new JavaFunction(Matrix4d.class, "translateLocal", true, Vector3dc.class));
            MAT_4.addMethod("posR_3", new JavaFunction(Matrix4d.class, "translateLocal", true, double.class, double.class, double.class));

            //Operator overloads
            registerHelperOverload("__add", "mat", "add");
            registerHelperOverload("__sub", "mat", "sub");
            registerHelperOverload("__mul", "mat", "mul");
            registerHelperOverload("__mul", "num", "smul");
            registerHelperOverload("__mulR", "num", "smul");
            registerHelperOverload("__mul", "vec", "vmul");

        } catch (Throwable t) {
            throw new RuntimeException("joml did a cringe :(", t);
        }
    }

    //Operator overloads

    public static Matrix2d add2(Matrix2d left, Matrix2d right) {
        return new Matrix2d(left).add(right);
    }
    public static Matrix3d add3(Matrix3d left, Matrix3d right) {
        return new Matrix3d(left).add(right);
    }
    public static Matrix4d add4(Matrix4d left, Matrix4d right) {
        return new Matrix4d(left).add(right);
    }

    public static Matrix2d sub2(Matrix2d left, Matrix2d right) {
        return new Matrix2d(left).sub(right);
    }
    public static Matrix3d sub3(Matrix3d left, Matrix3d right) {
        return new Matrix3d(left).sub(right);
    }
    public static Matrix4d sub4(Matrix4d left, Matrix4d right) {
        return new Matrix4d(left).sub(right);
    }

    public static Matrix2d mul2(Matrix2d left, Matrix2d right) {
        return new Matrix2d(left).mul(right);
    }
    public static Matrix3d mul3(Matrix3d left, Matrix3d right) {
        return new Matrix3d(left).mul(right);
    }
    public static Matrix4d mul4(Matrix4d left, Matrix4d right) {
        return new Matrix4d(left).mul(right);
    }

    public static Matrix2d smul2(Matrix2d left, double right) {
        return new Matrix2d(left).scale(right);
    }
    public static Matrix3d smul3(Matrix3d left, double right) {
        return new Matrix3d(left).scale(right);
    }
    public static Matrix4d smul4(Matrix4d left, double right) {
        return new Matrix4d(left)
                .scale(right) //Scale for mat4 gets *most* of it, but we also want to get the right column when doing scalar multiplication
                .m30(left.m30() * right)
                .m31(left.m31() * right)
                .m32(left.m32() * right)
                .m33(left.m33() * right);
    }

    public static Vector2d vmul2(Matrix2d left, Vector2d right) {
        return new Vector2d(right).mul(left);
    }
    public static Vector3d vmul3(Matrix3d left, Vector3d right) {
        return new Vector3d(right).mul(left);
    }
    public static Vector4d vmul4(Matrix4d left, Vector4d right) {
        return new Vector4d(right).mul(left);
    }

    //Other functions

    public static Matrix2d create2() {
        return new Matrix2d();
    }

    public static Matrix3d create3() {
        return new Matrix3d();
    }

    public static Matrix4d create4() {
        return new Matrix4d();
    }

    public static Vector2d getNum2(Matrix2d obj, int idx) {
        return obj.getColumn(idx, new Vector2d());
    }

    public static Vector3d getNum3(Matrix3d obj, int idx) {
        return obj.getColumn(idx, new Vector3d());
    }

    public static Vector4d getNum4(Matrix4d obj, int idx) {
        return obj.getColumn(idx, new Vector4d());
    }

    public static Object getStr2(Matrix2d obj, String indexer) {
        return switch (indexer) {
            case "c" -> new Matrix2d(obj);
            case "r0" -> obj.getRow(0, new Vector2d());
            case "r1" -> obj.getRow(1, new Vector2d());
            case "c0" -> obj.getColumn(0, new Vector2d());
            case "c1" -> obj.getColumn(1, new Vector2d());
            default -> throw new PetPetException("Attempt to index matrix2 with invalid key " + indexer);
        };
    }

    public static Object getStr3(Matrix3d obj, String indexer) {
        return switch (indexer) {
            case "c" -> new Matrix3d(obj);
            case "r0" -> obj.getRow(0, new Vector3d());
            case "r1" -> obj.getRow(1, new Vector3d());
            case "r2" -> obj.getRow(2, new Vector3d());
            case "c0" -> obj.getColumn(0, new Vector3d());
            case "c1" -> obj.getColumn(1, new Vector3d());
            case "c2" -> obj.getColumn(2, new Vector3d());
            default -> throw new PetPetException("Attempt to index matrix3 with invalid key " + indexer);
        };
    }

    public static Object getStr4(Matrix4d obj, String indexer) {
        return switch (indexer) {
            case "c" -> new Matrix4d(obj);
            case "r0" -> obj.getRow(0, new Vector4d());
            case "r1" -> obj.getRow(1, new Vector4d());
            case "r2" -> obj.getRow(2, new Vector4d());
            case "r3" -> obj.getRow(3, new Vector4d());
            case "c0" -> obj.getColumn(0, new Vector4d());
            case "c1" -> obj.getColumn(1, new Vector4d());
            case "c2" -> obj.getColumn(2, new Vector4d());
            case "c3" -> obj.getColumn(3, new Vector4d());

            case "v00" -> obj.m00();
            case "v01" -> obj.m01();
            case "v02" -> obj.m02();
            case "v03" -> obj.m03();
            case "v10" -> obj.m10();
            case "v11" -> obj.m11();
            case "v12" -> obj.m12();
            case "v13" -> obj.m13();
            case "v20" -> obj.m20();
            case "v21" -> obj.m21();
            case "v22" -> obj.m22();
            case "v23" -> obj.m23();
            case "v30" -> obj.m30();
            case "v31" -> obj.m31();
            case "v32" -> obj.m32();
            case "v33" -> obj.m33();

            default -> throw new PetPetException("Attempt to index matrix4 with invalid key " + indexer);
        };
    }

    public static Matrix2d scale2(Matrix2d obj, Object v) {
        if (v instanceof Double d)
            return obj.scale(d);
        else if (v instanceof Vector2d vec)
            return obj.scale(vec);
        throw new PetPetException("Attempt to call mat2.scale() with invalid arg, expected num or vec2");
    }

    public static Matrix3d scale3(Matrix3d obj, Object v) {
        if (v instanceof Double d)
            return obj.scale(d);
        else if (v instanceof Vector3d vec)
            return obj.scale(vec);
        throw new PetPetException("Attempt to call mat3.scale() with invalid arg, expected num or vec3");
    }

    public static Matrix4d scale4(Matrix4d obj, Object v) {
        if (v instanceof Double d)
            return obj.scale(d);
        else if (v instanceof Vector3d vec)
            return obj.scale(vec);
        throw new PetPetException("Attempt to call mat4.scale() with invalid arg, expected num or vec3");
    }

    public static Matrix3d rotXYZ3(Matrix3d mat, Vector3d vec) {
        return mat.rotateXYZ(vec.x, vec.y, vec.z);
    }

    public static Matrix3d rotZYX3(Matrix3d mat, Vector3d vec) {
        return mat.rotateZYX(vec.x, vec.y, vec.z);
    }

}
