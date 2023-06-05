package io.github.moonlightmaya.model.parts;

import org.joml.Vector2d;
import org.joml.Vector3d;
import petpet.external.PetPetWhitelist;

/**
 * An API for vertex manipulation through PetPet
 * Keeps track of a float[] and stores its index
 * in the array. Its methods manipulate the array.
 *
 * startIndex is the index into the float[] where
 * this vertex's data begins, not the index of the
 * vertex. For example, if the format of a part
 * has vertices of 8 floats in size, startIndex
 * will be 8 times the index of the vertex.
 */
@PetPetWhitelist
public record VertexAPI(float[] backingArray, int startIndex, int vertexSize) {

    @PetPetWhitelist
    public double index_0() {
        return startIndex / vertexSize;
    }

    @PetPetWhitelist
    public VertexAPI pos_3(double x, double y, double z) {
        backingArray[startIndex] = (float) x;
        backingArray[startIndex + 1] = (float) y;
        backingArray[startIndex + 2] = (float) z;
        return this;
    }

    @PetPetWhitelist
    public VertexAPI pos_1(Vector3d pos) {
        backingArray[startIndex] = (float) pos.x;
        backingArray[startIndex + 1] = (float) pos.y;
        backingArray[startIndex + 2] = (float) pos.z;
        return this;
    }

    @PetPetWhitelist
    public Vector3d pos_0() {
        return new Vector3d(backingArray[startIndex], backingArray[startIndex + 1], backingArray[startIndex + 2]);
    }

    @PetPetWhitelist
    public VertexAPI normal_3(double x, double y, double z) {
        backingArray[startIndex + 3] = (float) x;
        backingArray[startIndex + 4] = (float) y;
        backingArray[startIndex + 5] = (float) z;
        return this;
    }

    @PetPetWhitelist
    public VertexAPI normal_1(Vector3d normal) {
        backingArray[startIndex + 3] = (float) normal.x;
        backingArray[startIndex + 4] = (float) normal.y;
        backingArray[startIndex + 5] = (float) normal.z;
        return this;
    }

    @PetPetWhitelist
    public Vector3d normal_0() {
        return new Vector3d(backingArray[startIndex + 3], backingArray[startIndex + 4], backingArray[startIndex + 5]);
    }

    @PetPetWhitelist
    public VertexAPI uv_2(double x, double y) {
        backingArray[startIndex + 6] = (float) x;
        backingArray[startIndex + 7] = (float) y;
        return this;
    }

    @PetPetWhitelist
    public VertexAPI uv_1(Vector2d uv) {
        backingArray[startIndex + 6] = (float) uv.x;
        backingArray[startIndex + 7] = (float) uv.y;
        return this;
    }

    @PetPetWhitelist
    public Vector2d uv_0() {
        return new Vector2d(backingArray[startIndex + 6], backingArray[startIndex + 7]);
    }

    /**
     * The "get" variants of these functions
     * fetch the value into an existing vector.
     * This avoids unneeded allocations that come
     * from creating new vectors constantly.
     */

    @PetPetWhitelist
    public Vector3d getPos_1(Vector3d out) {
        return out.set(backingArray[startIndex], backingArray[startIndex + 1], backingArray[startIndex + 2]);
    }

    @PetPetWhitelist
    public Vector3d getNormal_1(Vector3d out) {
        return out.set(backingArray[startIndex + 3], backingArray[startIndex + 4], backingArray[startIndex + 5]);
    }

    @PetPetWhitelist
    public Vector2d getUV_1(Vector2d out) {
        return out.set(backingArray[startIndex + 6], backingArray[startIndex + 7]);
    }

}
