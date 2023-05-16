package io.github.moonlightmaya.script.vanilla;

import io.github.moonlightmaya.util.MathUtils;
import net.minecraft.client.model.ModelPart;
import org.joml.Matrix4d;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import petpet.external.PetPetWhitelist;
import petpet.types.PetPetTable;

import java.util.*;

/**
 * Meant to interface with elements of an EntityRenderer
 * or one of its subclasses.
 */
@PetPetWhitelist
public class VanillaRenderer {

    /**
     * Mixins will modify values in the top value of CURRENT_RENDERER.
     * The current renderer pushed when we begin rendering an entity,
     * and is popped when we finish rendering it.
     * The reason it's a stack is that it's possible that
     * entities could be rendered recursively using this feature (maybe?), so
     * we need to be able to handle multiple layers deep of renderers
     * at the same time.
     */
    public static final Deque<VanillaRenderer> CURRENT_RENDERER = new ArrayDeque<>();

    /**
     * The transformation applied to the entire model,
     * as calculated in LivingEntityRenderer.render().
     */
    public final Matrix4d aspectModelTransform = new Matrix4d();

    /**
     * Matrix containing the entire stack of vanilla transforms, including
     * the entity offset, the camera rotation/positioning, rotation of the
     * entire entity, and so on. Saved here as an intermediate while calculating
     * model part transforms.
     */
    public final Matrix4f savedVanillaModelTransform = new Matrix4f();

    /**
     * The transform applied *by aspect* to the vanilla model
     */
    public final Matrix4f appliedVanillaModelTransform = new Matrix4f();

    /**
     * The "render offset" of the entity, as applied in the entity render
     * dispatcher. Turns out that we don't need to use this while rendering,
     * but it's saved in case someone feels like knowing the value for some
     * reason.
     */
    public final Vector3d renderOffset = new Vector3d();

    /**
     * A helper stack of matrices which deals with canceling out rotations of parents
     * when vanilla model parts have children to render.
     */
    public final MathUtils.ActualMatrixStack modelPartsChildrenHelper = new MathUtils.ActualMatrixStack();

    /**
     * These maps store information about all the vanilla parts on the entity
     * wearing this aspect. These are calculated once for an aspect when it is
     * seen, and then not again until some kind of reload occurs.
     */
    public Map<Object, VanillaPart> vanillaParts = new HashMap<>();
    public Map<ModelPart, VanillaPart> vanillaPartInverse = new HashMap<>();

    /**
     * Generate the map from objects to root vanilla parts.
     *
     * Also generate an "inverse" map for faster lookups
     * of ModelPart -> VanillaPart for this VanillaRenderer.
     * There can't be a single global ModelPart -> VanillaPart map,
     * because each ModelPart may be shared by several VanillaParts
     * across multiple Aspects.
     *
     * TODO: call this function again when F3+T happens,
     * TODO: as the model part instances change and this becomes outdated
     */
    public void initVanillaParts(Map<Object, ModelPart> vanillaModel) {
        vanillaParts.clear();
        vanillaPartInverse.clear();
        //Fill vanilla part map from the vanilla model data
        for (Map.Entry<Object, ModelPart> entry : vanillaModel.entrySet()) {
            if (vanillaPartInverse.containsKey(entry.getValue())) {
                //Make sure to use the same underlying part for aliases
                vanillaParts.put(entry.getKey(), vanillaPartInverse.get(entry.getValue()));
            } else {
                VanillaPart newPart = new VanillaPart(entry.getValue());
                vanillaParts.put(entry.getKey(), newPart);
                newPart.traverse().forEach(p -> vanillaPartInverse.put(p.referencedPart, p));
            }
            vanillaParts.get(entry.getKey()).names.add(entry.getKey());
        }
        //Set the petpet field
        parts = new PetPetTable<>();
        parts.putAll(vanillaParts);
    }

    //PETPET METHODS
    public PetPetTable<Object, VanillaPart> parts; //Set by initVanillaParts()

    @PetPetWhitelist
    public PetPetTable<Object, VanillaPart> parts() {
        return parts;
    }

    @PetPetWhitelist
    public Vector3d getOffset() {
        return renderOffset;
    }

    @PetPetWhitelist
    public void matrix_1(Matrix4d mat) {
        appliedVanillaModelTransform.set(mat);
    }

    @PetPetWhitelist
    public Matrix4d matrix_0() {
        return aspectModelTransform;
    }

    @PetPetWhitelist
    public String __tostring() {
        return "VanillaRenderer";
    }
}
