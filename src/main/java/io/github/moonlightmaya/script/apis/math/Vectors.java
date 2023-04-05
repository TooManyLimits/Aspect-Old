package io.github.moonlightmaya.script.apis.math;

import org.joml.*;
import petpet.lang.run.JavaFunction;
import petpet.lang.run.PetPetClass;
import petpet.lang.run.PetPetException;

public class Vectors {

    public static final PetPetClass VEC_2, VEC_3, VEC_4;

    /**
     * Variety of helpers which were introduced to register for all three vectors at once
     * in certain cases, these are made to follow specific patterns which appear frequently
     * in JOML methods
     */
    private static void registerHelper(String petpetName, String javaName, Class<?>... args) {
        VEC_2.addMethod(petpetName, new JavaFunction(Vector2d.class, javaName, true, args));
        VEC_3.addMethod(petpetName, new JavaFunction(Vector3d.class, javaName, true, args));
        VEC_4.addMethod(petpetName, new JavaFunction(Vector4d.class, javaName, true, args));
    }

    private static void registerHelper(String petpetName, String javaName) {
        VEC_2.addMethod(petpetName, new JavaFunction(Vector2d.class, javaName, true));
        VEC_3.addMethod(petpetName, new JavaFunction(Vector3d.class, javaName, true));
        VEC_4.addMethod(petpetName, new JavaFunction(Vector4d.class, javaName, true));
    }

    private static void registerHelperCType(String petpetName, String javaName) {
        VEC_2.addMethod(petpetName, new JavaFunction(Vector2d.class, javaName, true, Vector2dc.class));
        VEC_3.addMethod(petpetName, new JavaFunction(Vector3d.class, javaName, true, Vector3dc.class));
        VEC_4.addMethod(petpetName, new JavaFunction(Vector4d.class, javaName, true, Vector4dc.class));
    }

    //Same for this pattern
    private static void registerHelperDType(String petpetName, String javaName) {
        VEC_2.addMethod(petpetName + "_2", new JavaFunction(Vector2d.class, javaName, true, double.class, double.class));
        VEC_3.addMethod(petpetName + "_3", new JavaFunction(Vector3d.class, javaName, true, double.class, double.class, double.class));
        VEC_4.addMethod(petpetName + "_4", new JavaFunction(Vector4d.class, javaName, true, double.class, double.class, double.class, double.class));
    }

    private static void registerHelperOverload(String petpetName, boolean isNum, String javaNameWithoutNumber) {
        VEC_2.addMethod(petpetName + "_" + (isNum ? "num" : "vec2"), new JavaFunction(Vectors.class, javaNameWithoutNumber + "2", false));
        VEC_3.addMethod(petpetName + "_" + (isNum ? "num" : "vec3"), new JavaFunction(Vectors.class, javaNameWithoutNumber + "3", false));
        VEC_4.addMethod(petpetName + "_" + (isNum ? "num" : "vec4"), new JavaFunction(Vectors.class, javaNameWithoutNumber + "4", false));
    }

    static {
        try {
            VEC_2 = new PetPetClass("vec2");
            VEC_3 = new PetPetClass("vec3");
            VEC_4 = new PetPetClass("vec4");

            //Length and length^2
            registerHelper("len", "length", new Class[0]);
            registerHelper("len2", "lengthSquared", new Class[0]);

            //Angle to other vector
            registerHelper("angle", "angle");

            //Distance
            registerHelperCType("dist_1", "distance");
            registerHelperDType("dist", "distance");
            registerHelperCType("dist2_1", "distanceSquared");
            registerHelperDType("dist2", "distanceSquared");

            //Vector3 exclusive
            VEC_3.addMethod("cross_1", new JavaFunction(Vector3d.class, "cross", true, Vector3dc.class));
            VEC_3.addMethod("cross_3", new JavaFunction(Vector3d.class, "cross", true, double.class, double.class, double.class));

            //Fields
            VEC_2.addField("x", Vector2d.class.getField("x"), false);
            VEC_2.addField("y", Vector2d.class.getField("y"), false);

            VEC_3.addField("x", Vector3d.class.getField("x"), false);
            VEC_3.addField("y", Vector3d.class.getField("y"), false);
            VEC_3.addField("z", Vector3d.class.getField("z"), false);

            VEC_4.addField("x", Vector4d.class.getField("x"), false);
            VEC_4.addField("y", Vector4d.class.getField("y"), false);
            VEC_4.addField("z", Vector4d.class.getField("z"), false);
            VEC_4.addField("w", Vector4d.class.getField("w"), false);

            //Get by index
            registerHelper("__get_num", "get", int.class);

            //Set function
            registerHelperCType("set_1", "set");
            registerHelperDType("set", "set");

            //Set operator with index
            registerHelperOverload("__set", true, "setByIndex");

            //Dot
            VEC_2.addMethod("dot_1", new JavaFunction(Vector2d.class, "dot", true));
            VEC_2.addMethod("dot_2", new JavaFunction(Vectors.class, "dot2", false)); //Joml forgor to add this one lol
            VEC_3.addMethod("dot_1", new JavaFunction(Vector3d.class, "dot", true, Vector3dc.class));
            VEC_3.addMethod("dot_3", new JavaFunction(Vector3d.class, "dot", true, double.class, double.class, double.class));
            VEC_4.addMethod("dot_1", new JavaFunction(Vector4d.class, "dot", true, Vector4dc.class));
            VEC_4.addMethod("dot_4", new JavaFunction(Vector4d.class, "dot", true, double.class, double.class, double.class, double.class));

            //Copy functionality is provided in the swizzle methods, do ".c"
            //Swizzle
            VEC_2.addMethod("__get_str", new JavaFunction(Vectors.class, "swizzle2", false));
            VEC_3.addMethod("__get_str", new JavaFunction(Vectors.class, "swizzle3", false));
            VEC_4.addMethod("__get_str", new JavaFunction(Vectors.class, "swizzle4", false));

            //Swizzle setters (eventually, i dont feel up to it rn)

            //In place modification functions
            registerHelper("zero", "zero");

            registerHelperCType("add_1", "add");
            registerHelperDType("add", "add");

            registerHelperCType("sub_1", "sub");
            registerHelperDType("sub", "sub");

            //JOML is extremely weird and they have lots and lots and lots of inconsistencies all over the place...
            //For instance, take the "div" function.
            //All of the other operations, add, sub, mul, they take a VectorNdc as their argument.
            //However, for div, it takes a VectorNd.
            //However, this is *only* for Vector2d and Vector3d. For Vector4d, the div function takes a "c" version.
            //It doesn't take the "c" version on the 2d and 3d vectors.
            //Also, the 4d vector doesn't have a version of the div() and mul() functions that take 4 doubles, while
            //the rest of the operations and dimensions do...
            //Stuff like this frustrates me.
            //Wish I could use my register helpers here, but guess not.

            registerHelperCType("mul_1", "mul"); //at least i have this one lone case
            VEC_2.addMethod("mul_2", new JavaFunction(Vector2d.class, "mul", true, double.class, double.class));
            VEC_3.addMethod("mul_3", new JavaFunction(Vector3d.class, "mul", true, double.class, double.class, double.class));
            VEC_4.addMethod("mul_4", new JavaFunction(Vectors.class, "mul4d", false));
            registerHelper("scale", "mul", double.class);

            VEC_2.addMethod("div_1", new JavaFunction(Vector2d.class, "div", true, Vector2d.class));
            VEC_3.addMethod("div_1", new JavaFunction(Vector3d.class, "div", true, Vector3d.class));
            VEC_4.addMethod("div_1", new JavaFunction(Vector4d.class, "div", true, Vector4dc.class));

            VEC_2.addMethod("div_2", new JavaFunction(Vector2d.class, "div", true, double.class, double.class));
            VEC_3.addMethod("div_3", new JavaFunction(Vector3d.class, "div", true, double.class, double.class, double.class));
            VEC_4.addMethod("div_4", new JavaFunction(Vectors.class, "div4d", false));

            //Operator overloads
            registerHelperOverload("__add", false, "add");
            registerHelperOverload("__sub", false, "sub");
            registerHelperOverload("__mul", false, "mul");

            JavaFunction smul2 = new JavaFunction(Vectors.class, "smul2", false);
            JavaFunction smul3 = new JavaFunction(Vectors.class, "smul3", false);
            JavaFunction smul4 = new JavaFunction(Vectors.class, "smul4", false);

            VEC_2.addMethod("__mul_num", smul2);
            VEC_3.addMethod("__mul_num", smul3);
            VEC_4.addMethod("__mul_num", smul4);
            VEC_2.addMethod("__mulR_num", smul2);
            VEC_3.addMethod("__mulR_num", smul3);
            VEC_4.addMethod("__mulR_num", smul4);

            registerHelperOverload("__div", false, "div");
            registerHelperOverload("__div", true, "sdiv");

            registerHelperOverload("__mod", false, "mod");
            registerHelperOverload("__mod", true, "smod");

            VEC_2.addMethod("__neg", new JavaFunction(Vectors.class, "neg2", false));
            VEC_3.addMethod("__neg", new JavaFunction(Vectors.class, "neg3", false));
            VEC_4.addMethod("__neg", new JavaFunction(Vectors.class, "neg4", false));

        } catch (Exception e) {
            throw new RuntimeException("joml has done a cringe :(", e);
        }
    }

    /**
     * Our own supplemental functions beyond this point, in places where
     * JOML didn't have one built in for the exact purpose we were after
     */

    //Dot with 2 doubles for 2d vectors since joml forgot to make it exist apparently
    public static double dot2(Vector2d v, double x, double y) {
        return v.x * x + v.y * y;
    }
    //Same for mul/div with 4 double for 4d vectors, they forgot those also
    public static Vector4d mul4d(Vector4d v, double x, double y, double z, double w) {
        return v.set(v.x * x, v.y * y, v.z * z, v.w * w);
    }
    public static Vector4d div4d(Vector4d v, double x, double y, double z, double w) {
        return v.set(v.x / x, v.y / y, v.z / z, v.w / w);
    }

    //Add
    public static Vector2d add2(Vector2d a, Vector2d b) {
        return new Vector2d(a.x + b.x, a.y + b.y);
    }
    public static Vector3d add3(Vector3d a, Vector3d b) {
        return new Vector3d(a.x + b.x, a.y + b.y, a.z + b.z);
    }
    public static Vector4d add4(Vector4d a, Vector4d b) {
        return new Vector4d(a.x + b.x, a.y + b.y, a.z + b.z, a.w + b.w);
    }
    //Sub
    public static Vector2d sub2(Vector2d a, Vector2d b) {
        return new Vector2d(a.x - b.x, a.y - b.y);
    }
    public static Vector3d sub3(Vector3d a, Vector3d b) {
        return new Vector3d(a.x - b.x, a.y - b.y, a.z - b.z);
    }
    public static Vector4d sub4(Vector4d a, Vector4d b) {
        return new Vector4d(a.x - b.x, a.y - b.y, a.z - b.z, a.w - b.w);
    }
    //Mul
    public static Vector2d mul2(Vector2d a, Vector2d b) {
        return new Vector2d(a.x * b.x, a.y * b.y);
    }
    public static Vector3d mul3(Vector3d a, Vector3d b) {
        return new Vector3d(a.x * b.x, a.y * b.y, a.z * b.z);
    }
    public static Vector4d mul4(Vector4d a, Vector4d b) {
        return new Vector4d(a.x * b.x, a.y * b.y, a.z * b.z, a.w * b.w);
    }
    //Scalar mul
    public static Vector2d smul2(Vector2d v, double s) {
        return new Vector2d(v.x * s, v.y * s);
    }
    public static Vector3d smul3(Vector3d v, double s) {
        return new Vector3d(v.x * s, v.y * s, v.z * s);
    }
    public static Vector4d smul4(Vector4d v, double s) {
        return new Vector4d(v.x * s, v.y * s, v.z * s, v.w * s);
    }
    //Div
    public static Vector2d div2(Vector2d a, Vector2d b) {
        return new Vector2d(a.x / b.x, a.y / b.y);
    }
    public static Vector3d div3(Vector3d a, Vector3d b) {
        return new Vector3d(a.x / b.x, a.y / b.y, a.z / b.z);
    }
    public static Vector4d div4(Vector4d a, Vector4d b) {
        return new Vector4d(a.x / b.x, a.y / b.y, a.z / b.z, a.w / b.w);
    }
    //Scalar div
    public static Vector2d sdiv2(Vector2d v, double s) {
        return new Vector2d(v.x / s, v.y / s);
    }
    public static Vector3d sdiv3(Vector3d v, double s) {
        return new Vector3d(v.x / s, v.y / s, v.z / s);
    }
    public static Vector4d sdiv4(Vector4d v, double s) {
        return new Vector4d(v.x / s, v.y / s, v.z / s, v.w / s);
    }

    //Mod
    public static Vector2d mod2(Vector2d a, Vector2d b) {
        return new Vector2d(a.x % b.x, a.y % b.y);
    }
    public static Vector3d mod3(Vector3d a, Vector3d b) {
        return new Vector3d(a.x % b.x, a.y % b.y, a.z % b.z);
    }
    public static Vector4d mod4(Vector4d a, Vector4d b) {
        return new Vector4d(a.x % b.x, a.y % b.y, a.z % b.z, a.w % b.w);
    }
    //Scalar mod
    public static Vector2d smod2(Vector2d v, double s) {
        return new Vector2d(v.x % s, v.y % s);
    }
    public static Vector3d smod3(Vector3d v, double s) {
        return new Vector3d(v.x % s, v.y % s, v.z % s);
    }
    public static Vector4d smod4(Vector4d v, double s) {
        return new Vector4d(v.x % s, v.y % s, v.z % s, v.w % s);
    }

    //Unary negation
    public static Vector2d neg2(Vector2d v) {
        return new Vector2d(-v.x, -v.y);
    }
    public static Vector3d neg3(Vector3d v) {
        return new Vector3d(-v.x, -v.y, -v.z);
    }
    public static Vector4d neg4(Vector4d v) {
        return new Vector4d(-v.x, -v.y, -v.z, -v.w);
    }

    //Setting by index
    public static double setByIndex2(Vector2d vec, int idx, double val) {
        switch (idx) {
            case 0 -> vec.x = val;
            case 1 -> vec.y = val;
            default -> throw new PetPetException("Invalid setter index for vec2: " + idx);
        }
        return val;
    }
    public static double setByIndex3(Vector3d vec, int idx, double val) {
        switch (idx) {
            case 0 -> vec.x = val;
            case 1 -> vec.y = val;
            case 2 -> vec.z = val;
            default -> throw new PetPetException("Invalid setter index for vec3: " + idx);
        }
        return val;
    }
    public static double setByIndex4(Vector4d vec, int idx, double val) {
        switch (idx) {
            case 0 -> vec.x = val;
            case 1 -> vec.y = val;
            case 2 -> vec.z = val;
            case 3 -> vec.w = val;
            default -> throw new PetPetException("Invalid setter index for vec4: " + idx);
        }
        return val;
    }


    private static double swizzle2Help(Vector2d swizzlee, char c) {
        return switch (c) {
            case 'x', 'r' -> swizzlee.x;
            case 'y', 'g' -> swizzlee.y;
            case '_' -> 0;
            default -> throw new PetPetException("Invalid swizzle for vec2, unrecognized character '" + c + "'");
        };
    }

    public static Object swizzle2(Vector2d swizzlee, String swizzler) {
        if (swizzler.equals("c")) //copy
            return new Vector2d(swizzlee);
        return switch (swizzler.length()) {
            case 2 -> new Vector2d(
                    swizzle2Help(swizzlee, swizzler.charAt(0)),
                    swizzle2Help(swizzlee, swizzler.charAt(1))
            );
            case 3 -> new Vector3d(
                    swizzle2Help(swizzlee, swizzler.charAt(0)),
                    swizzle2Help(swizzlee, swizzler.charAt(1)),
                    swizzle2Help(swizzlee, swizzler.charAt(2))
            );
            case 4 -> new Vector4d(
                    swizzle2Help(swizzlee, swizzler.charAt(0)),
                    swizzle2Help(swizzlee, swizzler.charAt(1)),
                    swizzle2Help(swizzlee, swizzler.charAt(2)),
                    swizzle2Help(swizzlee, swizzler.charAt(3))
            );
            default -> throw new PetPetException("Invalid swizzle for vec2: " + swizzler + ", must be length 2 to 4");
        };
    }

    private static double swizzle3Help(Vector3d swizzlee, char c) {
        return switch (c) {
            case 'x', 'r' -> swizzlee.x;
            case 'y', 'g' -> swizzlee.y;
            case 'z', 'b' -> swizzlee.z;
            case '_' -> 0;
            default -> throw new PetPetException("Invalid swizzle for vec3, unrecognized character '" + c + "'");
        };
    }

    public static Object swizzle3(Vector3d swizzlee, String swizzler) {
        if (swizzler.equals("c")) //copy
            return new Vector3d(swizzlee);
        return switch (swizzler.length()) {
            case 2 -> new Vector2d(
                    swizzle3Help(swizzlee, swizzler.charAt(0)),
                    swizzle3Help(swizzlee, swizzler.charAt(1))
            );
            case 3 -> new Vector3d(
                    swizzle3Help(swizzlee, swizzler.charAt(0)),
                    swizzle3Help(swizzlee, swizzler.charAt(1)),
                    swizzle3Help(swizzlee, swizzler.charAt(2))
            );
            case 4 -> new Vector4d(
                    swizzle3Help(swizzlee, swizzler.charAt(0)),
                    swizzle3Help(swizzlee, swizzler.charAt(1)),
                    swizzle3Help(swizzlee, swizzler.charAt(2)),
                    swizzle3Help(swizzlee, swizzler.charAt(3))
            );
            default -> throw new PetPetException("Invalid swizzle for vec3: " + swizzler + ", must be length 2 to 4");
        };
    }

    private static double swizzle4Help(Vector4d swizzlee, char c) {
        return switch (c) {
            case 'x', 'r' -> swizzlee.x;
            case 'y', 'g' -> swizzlee.y;
            case 'z', 'b' -> swizzlee.z;
            case 'w', 'a' -> swizzlee.w;
            case '_' -> 0;
            default -> throw new PetPetException("Invalid swizzle for vec4, unrecognized character '" + c + "'");
        };
    }

    public static Object swizzle4(Vector4d swizzlee, String swizzler) {
        if (swizzler.equals("c")) //copy
            return new Vector4d(swizzlee);
        return switch (swizzler.length()) {
            case 2 -> new Vector2d(
                    swizzle4Help(swizzlee, swizzler.charAt(0)),
                    swizzle4Help(swizzlee, swizzler.charAt(1))
            );
            case 3 -> new Vector3d(
                    swizzle4Help(swizzlee, swizzler.charAt(0)),
                    swizzle4Help(swizzlee, swizzler.charAt(1)),
                    swizzle4Help(swizzlee, swizzler.charAt(2))
            );
            case 4 -> new Vector4d(
                    swizzle4Help(swizzlee, swizzler.charAt(0)),
                    swizzle4Help(swizzlee, swizzler.charAt(1)),
                    swizzle4Help(swizzlee, swizzler.charAt(2)),
                    swizzle4Help(swizzlee, swizzler.charAt(3))
            );
            default -> throw new PetPetException("Invalid swizzle for vec4: " + swizzler + ", must be length 2 to 4");
        };
    }




}
