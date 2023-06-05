package io.github.moonlightmaya.script.vanilla;

import io.github.moonlightmaya.mixin.render.entity.LivingEntityRendererAccessor;
import io.github.moonlightmaya.util.MathUtils;
import io.github.moonlightmaya.util.RenderUtils;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.entity.Entity;
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
     * Set when this vanilla renderer needs an update.
     * This is true when we activate F3+T.
     */
    public boolean needsUpdate = false;
    public boolean initialized = false; //Set to true once this is initialized

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
     * This matrix transforms from entity space -> view space.
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
    public Map<String, VanillaPart> vanillaParts; //set in init
    public final Map<ModelPart, VanillaPart> vanillaPartInverse = new HashMap<>();

    //Analogous fields for feature renderers
    //Feature renderers don't have names normally, so a list could suffice,
    //but we may wish to provide some aliases for common feature renderers.
    public final Map<Double, VanillaFeature> featureRenderers = new HashMap<>();
    public final Map<FeatureRenderer<?, ?>, VanillaFeature> featureRendererInverse = new HashMap<>();

    /**
     * Initialize the part map for this user.
     * Generate the map from strings to root vanilla parts.
     *
     * Also generate an "inverse" map for faster lookups
     * of ModelPart -> VanillaPart for this VanillaRenderer.
     * There can't be a single global ModelPart -> VanillaPart map,
     * because each ModelPart may be shared by several VanillaParts
     * across multiple Aspects.
     */
    public void initVanillaParts(Entity user) {
        if (initialized)
            throw new IllegalStateException("Vanilla renderer initialized twice? Should not happen, please report to devs");

        //Get the renderer and root model part of this entity
        EntityRenderer<?> renderer = RenderUtils.getRenderer(user);
        List<ModelPart> roots = EntityRendererMaps.getEntityRoots(renderer);
        vanillaParts = VanillaPart.createTreeFromRoots(roots, vanillaPartInverse);

        //If it's a LivingEntityRenderer, wrap all the features and add them to the features list
        if (renderer instanceof LivingEntityRenderer<?,?>) {
            List<FeatureRenderer<?, ?>> featureRenderers = ((LivingEntityRendererAccessor) renderer).getFeatures();
            for (int i = 0; i < featureRenderers.size(); i++) {
                FeatureRenderer<?, ?> featureRenderer = featureRenderers.get(i);
                VanillaFeature vanillaFeature = new VanillaFeature(featureRenderer, vanillaPartInverse);
                this.featureRenderers.put((double) i, vanillaFeature);
                this.featureRendererInverse.put(featureRenderer, vanillaFeature);
            }
        }

        //Set the petpet fields
        parts.clear();
        parts.putAll(vanillaParts);
        features.clear();
        features.putAll(featureRenderers);

        //Mark initialized
        initialized = true;
    }

    /**
     * Update the renderer based on the user.
     * Only called if needsUpdate is true.
     *
     * The renderer needs an update if the underlying
     * ModelPart instances change, which happens after
     * the F3+T function is activated.
     */
    public void update(Entity user) {
        //Clear the inverse maps, as the vanilla object instances are changing
        vanillaPartInverse.clear();
        featureRendererInverse.clear();

        //Get the new entity renderer
        EntityRenderer<?> newEntityRenderer = RenderUtils.getRenderer(user);
        List<ModelPart> roots = EntityRendererMaps.getEntityRoots(newEntityRenderer);

        //If there are multiple roots, the strategy changes as there's another layer of tree.
        VanillaPart.updateTreeWithRoots(roots, this.vanillaParts, this.vanillaPartInverse);

        //If the renderer has feature renderer data, update those
        if (newEntityRenderer instanceof LivingEntityRenderer<?,?>) {
            List<FeatureRenderer<?, ?>> newFeatureRenderers = ((LivingEntityRendererAccessor) newEntityRenderer).getFeatures();
            for (int i = 0; i < newFeatureRenderers.size(); i++) {
                //For each feature renderer, update the value and repopulate the inverse map
                featureRenderers.get((double) i).update(newFeatureRenderers.get(i));
                featureRendererInverse.put(newFeatureRenderers.get(i), featureRenderers.get((double) i));
            }
        }

        //Update complete, no longer needed
        needsUpdate = false;
    }


    //PETPET METHODS
    public final PetPetTable<String, VanillaPart> parts = new PetPetTable<>(); //Set by initVanillaParts()
    public final PetPetTable<Double, VanillaFeature> features = new PetPetTable<>(); //Set by initVanillaParts()

    @PetPetWhitelist
    public PetPetTable<String, VanillaPart> parts() {
        return parts;
    }

    @PetPetWhitelist
    public PetPetTable<Double, VanillaFeature> features() {
        return features;
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
