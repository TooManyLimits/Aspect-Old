package io.github.moonlightmaya.vanilla;

import io.github.moonlightmaya.mixin.ModelPartAccessor;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelTransform;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import petpet.external.PetPetWhitelist;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Interface for an aspect to interact with vanilla ModelPart objects
 */
@PetPetWhitelist
public class VanillaPart {

    public final ModelPart referencedPart;

    private final Map<String, VanillaPart> children = new HashMap<>();

    /**
     * The transform applied _by vanilla_ to this part, which
     * we save and can then later query from code.
     */
    public final Matrix4f savedTransform = new Matrix4f();

    /**
     * The transform applied _to the vanilla part_ by aspect,
     * which can be modified through code.
     */
    public final Matrix4f appliedTransform = new Matrix4f();

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

    public VanillaPart(ModelPart part) {
        this.referencedPart = part;
        for (Map.Entry<String, ModelPart> child : ((ModelPartAccessor) (Object) part).getChildren().entrySet()) {
            children.put(child.getKey(), new VanillaPart(child.getValue()));
        }

        //Read the default transform and store its inverse for later
        readDefaultTransform();
    }

    public Stream<VanillaPart> traverse() {
        return Stream.concat(Stream.of(this), this.children.values().stream().flatMap(VanillaPart::traverse));
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

    // @PetPetWhitelist
    // public Matrix4d getMatrix() {/*TODO*/}

    // @PetPetWhitelist
    // public Matrix4d setMatrix() {/*TODO*/}

    @PetPetWhitelist
    public VanillaPart __get_str(String s) {
        return children.get(s);
    }
}
