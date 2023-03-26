package io.github.moonlightmaya.vanilla;

import net.minecraft.client.model.ModelPart;
import org.joml.Matrix4f;

import java.util.*;

/**
 * Meant to interface with elements of an EntityRenderer
 * or one of its subclasses.
 */
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
    public final Matrix4f aspectModelTransform = new Matrix4f();

    //Saved model transform as an intermediate storage step in the process
    //of calculating vanilla part matrices.
    public final Matrix4f savedVanillaModelTransform = new Matrix4f();

    /**
     * These maps store information about all the vanilla parts on the entity
     * wearing this aspect. These are calculated once for an aspect when it is
     * seen, and then not again until some kind of reload occurs.
     */
    public Map<Object, VanillaPart> vanillaParts;
    public Map<ModelPart, VanillaPart> vanillaPartInverse;

    public void initVanillaParts(Map<Object, ModelPart> vanillaModel) {
        //Create vanilla part map from the vanilla model data
        vanillaParts = new HashMap<>();
        for (Map.Entry<Object, ModelPart> entry : vanillaModel.entrySet()) {
            vanillaParts.put(entry.getKey(), new VanillaPart(entry.getValue()));
        }
        //Once that's done, also generate an "inverse" map for faster lookups
        //of ModelPart -> VanillaPart for this VanillaRenderer.
        //There can't be a global ModelPart -> VanillaPart map, because each model
        //part may be shared by several VanillaParts.
        vanillaPartInverse = new HashMap<>();
        for (VanillaPart vanillaPart : vanillaParts.values()) {
            vanillaPart.traverse().forEach(p -> vanillaPartInverse.put(p.referencedPart, p));
        }
    }


}
