package io.github.moonlightmaya.model;

import org.joml.*;
import petpet.external.PetPetWhitelist;
import petpet.types.PetPetList;

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
    public final Vector3d partPivot = new Vector3d();
    public final Vector3d partPos = new Vector3d();
    public final Quaterniond partRot = new Quaterniond();
    public final Vector3d partScale = new Vector3d(1, 1, 1);
    public boolean visible = true;

    /**
     * A list of objects which will act on the transformable's matrices,
     * and modify them if need be.
     */
    public PetPetList<Transformer> transformers = new PetPetList<>();

    //Whether this needs its matrix recalculated. After calling rot(), pos(), etc. this will be set to true.
    //If it's true at rendering time, then the matrix field will be updated, and this will be set to false.
    public boolean needsMatrixRecalculation = true;

    public void cloneFrom(Transformable other) {
        partPos.set(other.partPos);
        partRot.set(other.partRot);
        partScale.set(other.partScale);
        partPivot.set(other.partPivot);
        positionMatrix.set(other.positionMatrix);
        normalMatrix.set(other.normalMatrix);
        needsMatrixRecalculation = other.needsMatrixRecalculation;
        visible = other.visible;
        transformers = other.transformers;
    }

    //Avoid unneeded allocations
    private final Quaternionf tempRot = new Quaternionf();

    //Synchronize to avoid multiple simultaneous accesses to tempRot
    public synchronized void recalculateMatrixIfNeeded() {
        //If needsMatrixRecalculation, or any of the transformers
        //demand it, we should recalculate
        boolean shouldRecalculate = needsMatrixRecalculation;
        if (!shouldRecalculate) {
            for (Transformer t : transformers)
                if (t.forceRecalculation()) {
                    shouldRecalculate = true;
                    break;
                }
        }

        //If we should recalculate, then... do it
        if (shouldRecalculate) {
            //Scale down the pivot value, it's in "block" units
            positionMatrix.translation((float) partPivot.x / 16, (float) partPivot.y / 16, (float) partPivot.z / 16);

            //Arbitrarily decide that other transformers should happen
            //before the ones imposed by the pos, rot, scale etc of this
            //class. There is a case to be made for abstracting away
            //this "transformable" class's actions as yet another transformer,
            //but we're not worrying about that right now.

            for (Transformer t : transformers) {
                if (t != null) {
                    //If the method returns true, cancel further transformers
                    if (t.affectMatrix(positionMatrix))
                        break;
                }
            }

            tempRot.set(partRot);
            positionMatrix
                    .rotate(tempRot)
                    .scale((float) partScale.x, (float) partScale.y, (float) partScale.z)
                    .translate((float) partPos.x / 16, (float) partPos.y / 16, (float) partPos.z / 16)
                    .translate((float) -partPivot.x / 16, (float) -partPivot.y / 16, (float) -partPivot.z / 16);

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
        partPos.set(x, y, z);
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
        partRot.identity().rotationZYX(z * s, y * s, x * s);
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
        return partPos;
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
        return partRot.getEulerAnglesXYZ(new Vector3d());
    }

    @PetPetWhitelist
    public Transformable quat_1(Quaterniond quat) {
        setRot(quat);
        return this;
    }
    @PetPetWhitelist
    public Quaterniond quat_0() {
        return partRot;
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
        return partScale;
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
        return partPivot;
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

    /**
     * Mark the matrix as dirty, so it needs
     * to be recalculated
     * This may happen if you directly modify
     * a vector or quaternion involved in the
     * matrix calculation.
     */
    @PetPetWhitelist
    public Transformable dirty() {
        this.needsMatrixRecalculation = true;
        return this;
    }

    @PetPetWhitelist
    public Matrix4d rawMatrix() {
        return new Matrix4d(positionMatrix);
    }

    @PetPetWhitelist
    public Transformable visible_1(boolean b) {
        this.visible = b;
        return this;
    }

    @PetPetWhitelist
    public boolean visible_0() {
        return visible;
    }

    @PetPetWhitelist
    public PetPetList<Transformer> transformers() {
        return transformers;
    }

    /**
     * Something which can act on a transformable and modify its
     * matrices.
     * - Vanilla parts
     * - Animations
     */
    public interface Transformer {
        /**
         * @return true if this transformer should force
         * the part it's on to recalculate
         */
        boolean forceRecalculation();

        /**
         * Affect the passed matrix.
         * If it returns true, then further transformers
         * on the part will be canceled.
         */
        boolean affectMatrix(Matrix4f matrix);
    }

}
