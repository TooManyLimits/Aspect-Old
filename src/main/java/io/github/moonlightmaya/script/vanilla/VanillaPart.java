package io.github.moonlightmaya.script.vanilla;

import io.github.moonlightmaya.Aspect;
import io.github.moonlightmaya.mixin.render.vanilla.part.ModelPartAccessor;
import io.github.moonlightmaya.model.Transformable;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelTransform;
import org.joml.Matrix4d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import petpet.external.PetPetWhitelist;
import petpet.types.PetPetTable;
import petpet.types.immutable.PetPetTableView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Interface for an aspect to interact with vanilla ModelPart objects
 */
@PetPetWhitelist
public class VanillaPart implements Transformable.Transformer {

    public ModelPart referencedPart;
    public final String name;
    public VanillaRenderer renderer;

    private final HashMap<String, VanillaPart> childrenBacking = new PetPetTable<>(); //backing map for the children

    /**
     * When we press F3+T, the entity renderers reload, and the instances
     * of ModelPart change. As such, we need to update our VanillaPart
     * instances to match the new model parts.
     */
    public void update(ModelPart newPart) {
        //Name shouldn't change
        this.referencedPart = newPart;
        for (Map.Entry<String, ModelPart> childEntry : ((ModelPartAccessor) (Object) newPart).getChildren().entrySet()) {
            String childName = childEntry.getKey();
            ModelPart childPart = childEntry.getValue();
            VanillaPart vanillaChild = childrenBacking.get(childName);
            vanillaChild.update(childPart);
        }
    }

    /**
     * The transform applied _by vanilla_ to this part, which
     * we save and can then later query from code.
     */
    public final Matrix4d savedTransform = new Matrix4d();

    /**
     * The transform applied _to the vanilla part_ by aspect,
     * which can be modified through code.
     */
    public final Matrix4d appliedTransform = new Matrix4d();

    /**
     * Whether to override the visibility of the
     * model part
     */
    public Boolean appliedVisibility = null;

    /**
     * The saved vanilla part's visibility, for *reading* purposes
     */
    public boolean savedVisibility = true;

    /**
     * The inverse of the "default transform" of the model part.
     * Calculate once upon creation of the VanillaPart.
     *
     * Dust message:
     * First apply the inverse default. If the model is in its default state in Blockbench,
     * this will reverse that. For instance, it will stuff the player’s arms and neck into their body, like prewrite player models did.
     * Then apply the saved matrix. This will move the parts from this “stuffed” position into the actual, correct one.
     * This should work so long as the entity in Blockbench is shaped the same way as the “default transforms”
     * in the code set it up to be.
     * Optifine’s CEM does not obey this rule, but it’s the only sensible way to have this work, I believe.
     *
     */
    public final Matrix4f inverseDefaultTransform = new Matrix4f();

    public VanillaPart(VanillaRenderer renderer, String name, ModelPart part) {
        this.renderer = renderer;
        this.name = name;
        this.referencedPart = part;

        for (Map.Entry<String, ModelPart> child : ((ModelPartAccessor) (Object) part).getChildren().entrySet()) {
            childrenBacking.put(child.getKey(), new VanillaPart(renderer, child.getKey(), child.getValue()));
        }

        //Read the default transform and store its inverse for later
        readDefaultTransform();

        //Store the petpet field
        children = new PetPetTableView<>(childrenBacking);
    }

    public Stream<VanillaPart> traverse() {
        return Stream.concat(Stream.of(this), this.childrenBacking.values().stream().flatMap(VanillaPart::traverse));
    }

    /**
     * Read the default transform, calculate its inverse, and store.
     * Some conversions are done because Minecraft's axes are a bit different
     * from Blockbench's.
     */
    private void readDefaultTransform() {
        ModelTransform defaultTransform = referencedPart.getDefaultTransform();
        float pitch = -defaultTransform.pitch;
        float yaw = -defaultTransform.yaw;
        float roll = defaultTransform.roll;

        float pivotX = -defaultTransform.pivotX;
        float pivotY = -defaultTransform.pivotY;
        float pivotZ = defaultTransform.pivotZ;

        inverseDefaultTransform.translation(pivotX / 16.0f, pivotY / 16.0f, pivotZ / 16.0f);
        if (pitch != 0.0f || yaw != 0.0f || roll != 0.0f) {
            inverseDefaultTransform.rotate(new Quaternionf().rotationZYX(roll, yaw, pitch));
        }
        inverseDefaultTransform.invert();
    }

    @PetPetWhitelist
    public VanillaPart __get_str(String s) {
        return childrenBacking.get(s);
    }

    public final PetPetTableView<String, VanillaPart> children;

    @PetPetWhitelist
    public final PetPetTableView<String, VanillaPart> children_0() {
        return children;
    }

    @PetPetWhitelist
    public String __tostring() {
        return "VanillaPart(name=" + name + ")";
    }

    @PetPetWhitelist
    public Matrix4d matrix_0() {
        return savedTransform;
    }

    @PetPetWhitelist
    public VanillaPart matrix_1(Matrix4d mat) {
        appliedTransform.set(mat);
        return this;
    }

    @PetPetWhitelist
    public boolean visible_0() {
        return savedVisibility;
    }

    @PetPetWhitelist
    public VanillaPart visible_1(Boolean b) {
        appliedVisibility = b;
        return this;
    }

    /**
     * Always recalculate matrices for vanilla parts
     */
    @Override
    public boolean forceRecalculation() {
        return true;
    }

    //Recalculation of the matrix
    //Assume that *only one thing is being rendered at a time* (one thread)
    //Otherwise, this static variable will cause bad things.
    //However, since rendering is single threaded, accessing this variable
    //statically does not cause issues.
    private static final Matrix4f tempMatrixSavedTransform = new Matrix4f();

    /**
     * First undo the "default" transformation by multiplying the inverse
     * default transform, then add the saved transform to the part.
     */
    @Override
    public boolean affectMatrix(Matrix4f matrix) {
        matrix.mul(inverseDefaultTransform);
        tempMatrixSavedTransform.set(savedTransform);
        matrix.mul(tempMatrixSavedTransform);
        return false;
    }
}
