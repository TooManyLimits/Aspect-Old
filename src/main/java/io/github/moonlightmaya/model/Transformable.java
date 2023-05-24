package io.github.moonlightmaya.model;

import io.github.moonlightmaya.script.vanilla.VanillaPart;
import org.joml.*;
import petpet.external.PetPetWhitelist;

import java.lang.Math;

/**
 * A base class for things which can have their position and rendering
 * transformed using a matrix.
 * Also lumps in some other useful things, such as visibility
 * - Model parts
 * - Render tasks
 */
@PetPetWhitelist
public abstract class Transformable {

    public final Matrix4f positionMatrix = new Matrix4f();
    public final Matrix3f normalMatrix = new Matrix3f();
    public final Vector3f partPivot = new Vector3f();
    public final Vector3f partPos = new Vector3f();
    public final Quaternionf partRot = new Quaternionf();
    public final Vector3f partScale = new Vector3f(1, 1, 1);
    public boolean visible = true;
    public VanillaPart vanillaParent; //The vanilla part that this will take transforms from

    //Whether this needs its matrix recalculated. After calling rot(), pos(), etc. this will be set to true.
    //If it's true at rendering time, then the matrix field will be updated, and this will be set to false.
    public boolean needsMatrixRecalculation = true;


    //Recalculation of the matrix
    //Assume that *only one thing is being rendered at a time* (one thread)
    //Otherwise, this static variable will cause bad things.
    private static final Matrix4f tempMatrixSavedTransform = new Matrix4f();
    protected void recalculateMatrixIfNeeded() {
        if (needsMatrixRecalculation || vanillaParent != null) {
            //Scale down the pivot value, it's in "block" units
            positionMatrix.translation(partPivot.mul(1f/16));

            if (vanillaParent != null) {
                positionMatrix.mul(vanillaParent.inverseDefaultTransform);
                tempMatrixSavedTransform.set(vanillaParent.savedTransform);
                positionMatrix.mul(tempMatrixSavedTransform);
            }

            positionMatrix
                    .rotate(partRot)
                    .scale(partScale)
                    .translate(partPos)
                    .translate(-partPivot.x, -partPivot.y, -partPivot.z);

            //Scale the pivot value back up again
            partPivot.mul(16f);
            //Compute the normal matrix as well and store it
            positionMatrix.normal(normalMatrix);
            //Matrices are now calculated, don't need to be recalculated anymore for this object
            needsMatrixRecalculation = false;
        }
    }

    public void setPos(Vector3f vec) {
        setPos(vec.x, vec.y, vec.z);
    }
    public void setPos(Vector3d vec) {
        setPos((float) vec.x, (float) vec.y, (float) vec.z);
    }

    public void setPos(float x, float y, float z) {
        partPos.set(x / 16f, y / 16f, z / 16f);
        needsMatrixRecalculation = true;
    }

    public void setPivot(Vector3f vec) {
        setPivot(vec.x, vec.y, vec.z);
    }

    public void setPivot(float x, float y, float z) {
        partPivot.set(x, y, z);
        needsMatrixRecalculation = true;
    }

    public void setScale(Vector3f vec) {
        setScale(vec.x, vec.y, vec.z);
    }

    public void setScale(float s) {
        setScale(s, s, s);
    }

    public void setScale(float x, float y, float z) {
        partScale.set(x, y, z);
        needsMatrixRecalculation = true;
    }

    public void setRot(Vector3f vec) {
        setRot(vec.x, vec.y, vec.z);
    }

    public void setRot(float x, float y, float z) {
        float s = (float) (Math.PI / 180);
        partRot.identity().rotationXYZ(x * s, y * s, z * s);
        needsMatrixRecalculation = true;
    }

    public void setRot(Quaterniond rot) {
        partRot.set(rot);
        needsMatrixRecalculation = true;
    }

    @PetPetWhitelist
    public Transformable pos_3(double x, double y, double z) {
        setPos((float) x, (float) y, (float) z);
        return this;
    }
    @PetPetWhitelist
    public Transformable pos_1(Vector3d v) {
        return pos_3(v.x, v.y, v.z);
    }
    @PetPetWhitelist
    public Vector3d pos_0() {
        return new Vector3d(partPos).mul(16d);
    }

    @PetPetWhitelist
    public Transformable rot_3(double x, double y, double z) {
        setRot((float) x, (float) y, (float) z);
        return this;
    }
    @PetPetWhitelist
    public Transformable rot_1(Vector3d v) {
        return rot_3(v.x, v.y, v.z);
    }

    @PetPetWhitelist
    public Vector3d rot_0() {
        return new Vector3d(partRot.getEulerAnglesXYZ(new Vector3f()));
    }

    @PetPetWhitelist
    public Transformable quat_1(Quaterniond quat) {
        setRot(quat);
        return this;
    }
    @PetPetWhitelist
    public Quaterniond quat_0() {
        return new Quaterniond(partRot);
    }

    @PetPetWhitelist
    public Transformable scale_3(double x, double y, double z) {
        setScale((float) x, (float) y, (float) z);
        return this;
    }
    @PetPetWhitelist
    public Transformable scale_1(Vector3d v) {
        return scale_3(v.x, v.y, v.z);
    }
    @PetPetWhitelist
    public Vector3d scale_0() {
        return new Vector3d(partScale);
    }
    @PetPetWhitelist
    public Transformable piv_3(double x, double y, double z) {
        setPivot((float) x, (float) y, (float) z);
        return this;
    }
    @PetPetWhitelist
    public Transformable piv_1(Vector3d v) {
        return piv_3(v.x, v.y, v.z);
    }
    @PetPetWhitelist
    public Vector3d piv_0() {
        return new Vector3d(partPivot);
    }

    @PetPetWhitelist
    public Transformable matrix_1(Matrix4d mat) {
        this.positionMatrix.set(mat);
        this.positionMatrix.normal(normalMatrix);
        partPos.zero();
        partRot.identity();
        partPivot.zero();
        partScale.set(1);
        this.needsMatrixRecalculation = false;
        return this;
    }

    @PetPetWhitelist
    public Matrix4d matrix_0() {
        recalculateMatrixIfNeeded(); //recalculate before
        return rawMatrix();
    }

    @PetPetWhitelist
    public Matrix4d rawMatrix() {
        return new Matrix4d(positionMatrix);
    }

    @PetPetWhitelist
    public Transformable visible_1(Boolean b) {
        this.visible = b;
        return this;
    }

    @PetPetWhitelist
    public Boolean visible_0() {
        return visible;
    }

    @PetPetWhitelist
    public Transformable vanillaParent_1(VanillaPart vanillaPart) {
        vanillaParent = vanillaPart;
        return this;
    }

    @PetPetWhitelist
    public VanillaPart vanillaParent_0() {
        return vanillaParent;
    }

}
