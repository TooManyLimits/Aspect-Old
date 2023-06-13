package io.github.moonlightmaya.script.apis.math;

import org.joml.*;
import petpet.external.PetPetReflector;
import petpet.external.PetPetWhitelist;
import petpet.lang.run.JavaFunction;
import petpet.lang.run.PetPetClass;
import petpet.lang.run.PetPetException;

import java.lang.Math;

/**
 * Only one size of quaternion ofc but want to give api
 */
@PetPetWhitelist
public class Quaternions {

    public static final PetPetClass QUATERNION_CLASS;

    public static final JavaFunction QUAT_CREATE = new JavaFunction(Quaternions.class, "create", false);

    static {
        QUATERNION_CLASS = PetPetReflector.reflect(Quaternions.class, "quat");

        //Setters
        QUATERNION_CLASS.addMethod("set_4", new JavaFunction(Quaterniond.class, "set", true, double.class, double.class, double.class, double.class));
        QUATERNION_CLASS.addMethod("set_1", new JavaFunction(Quaterniond.class, "set", true, Quaterniondc.class));

        //Modifying
        JavaFunction normalizer = new JavaFunction(Quaterniond.class, "normalize", true, new Class[0]);
        QUATERNION_CLASS.addMethod("normalize", normalizer);
        QUATERNION_CLASS.addMethod("fix", normalizer);
        QUATERNION_CLASS.addMethod("inv", new JavaFunction(Quaterniond.class, "invert", true, new Class[0]));
        QUATERNION_CLASS.addMethod("conj", new JavaFunction(Quaterniond.class, "conjugate", true, new Class[0]));
        QUATERNION_CLASS.addMethod("slerp", new JavaFunction(Quaterniond.class, "slerp", true, Quaterniondc.class, double.class));
        QUATERNION_CLASS.addMethod("mul", new JavaFunction(Quaterniond.class, "mul", true, Quaterniondc.class));
        QUATERNION_CLASS.addMethod("mulR", new JavaFunction(Quaterniond.class, "premul", true, Quaterniondc.class));

        //Filling with definite data
        QUATERNION_CLASS.addMethod("axisAngle_2", new JavaFunction(Quaterniond.class, "fromAxisAngleRad", true, Vector3dc.class, double.class));
        QUATERNION_CLASS.addMethod("axisAngle_4", new JavaFunction(Quaterniond.class, "fromAxisAngleRad", true, double.class, double.class, double.class, double.class));
        QUATERNION_CLASS.addMethod("axisAngleDeg_2", new JavaFunction(Quaterniond.class, "fromAxisAngleDeg", true, Vector3dc.class, double.class));
        QUATERNION_CLASS.addMethod("axisAngleDeg_4", new JavaFunction(Quaterniond.class, "fromAxisAngleDeg", true, double.class, double.class, double.class, double.class));
        QUATERNION_CLASS.addMethod("lookAt_2", new JavaFunction(Quaterniond.class, "lookAlong", true, Vector3dc.class, Vector3dc.class));
        QUATERNION_CLASS.addMethod("lookAt_6", new JavaFunction(Quaterniond.class, "lookAlong", true, double.class, double.class, double.class, double.class, double.class, double.class));
        QUATERNION_CLASS.addMethod("reset", new JavaFunction(Quaterniond.class, "identity", true));

        //Reading info
        QUATERNION_CLASS.addMethod("len2", new JavaFunction(Quaterniond.class, "lengthSquared", true, new Class[0]));
        QUATERNION_CLASS.addMethod("angle", new JavaFunction(Quaterniond.class, "angle", true, new Class[0]));
    }

    public static Quaterniond create() {
        return new Quaterniond();
    }

    @PetPetWhitelist
    public static Quaterniond rotXYZ_3(Quaterniond quat, double x, double y, double z) {
        return quat.rotateXYZ(x, y, z);
    }

    @PetPetWhitelist
    public static Quaterniond rotZYX_3(Quaterniond quat, double x, double y, double z) {
        return quat.rotateZYX(z, y, x);
    }

    @PetPetWhitelist
    public static Quaterniond rotYXZ_3(Quaterniond quat, double x, double y, double z) {
        return quat.rotateYXZ(y, x, z);
    }

    @PetPetWhitelist
    public static Quaterniond rotXYZ_1(Quaterniond quat, Vector3d xyz) {
        return quat.rotateXYZ(xyz.x, xyz.y, xyz.z);
    }

    @PetPetWhitelist
    public static Quaterniond rotZYX_1(Quaterniond quat, Vector3d xyz) {
        return quat.rotateZYX(xyz.z, xyz.y, xyz.x);
    }

    @PetPetWhitelist
    public static Quaterniond rotYXZ_1(Quaterniond quat, Vector3d xyz) {
        return quat.rotateYXZ(xyz.y, xyz.x, xyz.z);
    }


    /**
     * Getters and setters
     */
    @PetPetWhitelist
    public static Object __get_str(Quaterniond self, String other) {
        return switch (other) {
            case "c" -> new Quaterniond(self);
            case "x" -> self.x;
            case "y" -> self.y;
            case "z" -> self.z;
            case "w", "a" -> self.w;
            default -> throw new PetPetException("Attempt to get from quaternion with invalid key " + other);
        };
    }

    @PetPetWhitelist
    public static Object __get_num(Quaterniond self, double other) {
        int key = (int) other;
        return switch (key) {
            case 0 -> self.x;
            case 1 -> self.y;
            case 2 -> self.z;
            case 3 -> self.w;
            default -> throw new PetPetException("Attempt to get from quaternion with invalid key " + other);
        };
    }

    @PetPetWhitelist
    public static void __set_str(Quaterniond self, String key, Object other) {
        if (other instanceof Double v)
            switch (key) {
                case "x" -> self.x = v;
                case "y" -> self.y = v;
                case "z" -> self.z = v;
                case "w", "a" -> self.w = v;
                default -> throw new PetPetException("Attempt to set in quaternion with invalid key " + other);
            }
        else
            throw new PetPetException("Attempt to set in quaternion with invalid value " + other + ", expected number");
    }

    @PetPetWhitelist
    public static void __set_num(Quaterniond self, double key, Object other) {
        if (other instanceof Double v)
            switch ((int) key) {
                case 0 -> self.x = v;
                case 1 -> self.y = v;
                case 2 -> self.z = v;
                case 3 -> self.w = v;
                default -> throw new PetPetException("Attempt to set in quaternion with invalid key " + other);
            }
        else
            throw new PetPetException("Attempt to set in quaternion with invalid value " + other + ", expected number");
    }

    //Matrix conversions
    @PetPetWhitelist
    public static Matrix3d asMat3(Quaterniond self) {
        return new Matrix3d().set(self);
    }
    @PetPetWhitelist
    public static Matrix4d asMat4(Quaterniond self) {
        return new Matrix4d().set(self);
    }

    //Euler angle conversions
    @PetPetWhitelist
    public static Vector3d asXYZ(Quaterniond self) {
        return self.getEulerAnglesXYZ(new Vector3d());
    }
    @PetPetWhitelist
    public static Vector3d asZYX(Quaterniond self) {
        return self.getEulerAnglesZYX(new Vector3d());
    }

    //length (just sqrt of length squared, not built in)
    @PetPetWhitelist
    public static double len(Quaterniond self) {
        return Math.sqrt(self.lengthSquared());
    }

    //multiplying
    @PetPetWhitelist
    public static Quaterniond __mul_quat(Quaterniond self, Quaterniond other) {
        return new Quaterniond(self).mul(other);
    }
    @PetPetWhitelist
    public static Vector3d __mul_vec3(Quaterniond self, Vector3d other) {
        return new Vector3d(other).rotate(self);
    }
    @PetPetWhitelist
    public static Vector4d __mul_vec4(Quaterniond self, Vector4d other) {
        return new Vector4d(other).rotate(self);
    }

}
