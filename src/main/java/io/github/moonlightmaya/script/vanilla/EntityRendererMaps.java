package io.github.moonlightmaya.script.vanilla;

import com.google.common.collect.ImmutableList;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.entity.EntityType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class deals with the various global state of vanilla model parts
 * and entity renderers. It keeps track of mapping from EntityRenderer
 * instances to their associated vanilla model part trees.
 *
 * See mixin/render/vanilla/sort/EntityRenderersMixin for more info.
 */
public class EntityRendererMaps {


    private static ListeningState listeningState = ListeningState.IDLE;
    private static int numRootsLeft = 0;

    private static final List<ModelPart> storedRoots = new ArrayList<>();

    /**
     * As we receive model parts in the FEATURE listening state,
     * add them to the storedFeatureRendererModelParts, then
     * flush these when the feature renderer is finished, and
     * store in FEATURE_RENDERER_PARTS
     */
    private static final List<ModelPart> storedFeatureRendererModelParts = new ArrayList<>();

    /**
     * The current set of vanilla model parts, indexed
     * by their EntityRenderer. Modified by various mixins
     * when needed.
     */
    private static final Map<EntityRenderer<?>, @Nullable List<ModelPart>> MODEL_PARTS = new HashMap<>();
    private static final Map<FeatureRenderer<?, ?>, @Nullable List<ModelPart>> FEATURE_RENDERER_PARTS = new HashMap<>();

    /**
     * Clear out the maps. Used when the game is reloaded
     * and the entity renderer instances change.
     */
    public static void clear() {
        MODEL_PARTS.clear();
        FEATURE_RENDERER_PARTS.clear();
        storedRoots.clear();
        storedFeatureRendererModelParts.clear();
        numRootsLeft = 0;
        listeningState = ListeningState.IDLE;
    }

    /**
     * This is an important map for other mods adding custom entities. All you have to do is put()
     * your EntityType key into this map. The integer value is explained as follows:
     *
     * Every vanilla LivingEntityRenderer in the game follows this same structured pattern:
     * - Create all the model parts for the actual entity model
     * THEN
     * - Create feature renderers and add them to the LivingEntityRenderer
     *
     * Aspect assumes that your mod follows this same format. Now:
     * In the case of *almost all entities*, there is only ONE model part for the actual entity model.
     * There are two exceptions (I believe): Pufferfish and Tropical fish. If you look at the decompiled
     * code for their entity renderers, you will see that
     * - The Pufferfish creates THREE models before creating its FeatureRenderers (it has no feature renderers).
     * - The Tropical Fish creates TWO models before creating its FeatureRenderers (it has one feature renderer).
     * Every other entity in the game creates either one model before the FeatureRenderers, or none.
     * If your entity creates more than one model before the FeatureRenderers are made, then input that number
     * into this map just as it's defined for pufferfish and tropical fish.
     */
    public static final Map<EntityType<?>, Integer> NUMBER_OF_ROOTS_BEFORE_FEATURE_RENDERERS = new HashMap<>() {{
        put(EntityType.PUFFERFISH, 3);
        put(EntityType.TROPICAL_FISH, 2);
    }};

    /**
     * Primes the entity renderer maps, awaiting new getPart() calls
     */
    public static void prime(EntityType<?> entityType) {
        numRootsLeft = NUMBER_OF_ROOTS_BEFORE_FEATURE_RENDERERS.getOrDefault(entityType, 1);
        listeningState = ListeningState.MODEL;
        //Maybe we clear these maps a bit excessively often, but it's good to be on the safe side
        storedRoots.clear();
        storedFeatureRendererModelParts.clear();
    }

    public static void onPartCreated(ModelPart part) {
        switch (listeningState) {
            case IDLE -> {}
            case MODEL -> {
                //Save this model part, later when we complete() the renderer
                //we will save it in there
                storedRoots.add(part);
                //Decrement the number of remaining roots
                numRootsLeft--;
                //If we're out of roots to find, then start gathering features
                if (numRootsLeft == 0)
                    //We got all the roots, start looking for features now
                    listeningState = ListeningState.FEATURE;
            }
            case FEATURE -> storedFeatureRendererModelParts.add(part);
        }
    }

    /**
     * When we complete an entity renderer, save the values and
     * reset the state to await the next entity renderer creation
     */
    public static void completeEntityRenderer(EntityRenderer<?> renderer) {
        //Complete the renderer entry using our stored values
        if (!storedRoots.isEmpty()) {
            ArrayList<ModelPart> parts = new ArrayList<>(storedRoots.size());
            parts.addAll(storedRoots);
            MODEL_PARTS.put(renderer, parts);
            storedRoots.clear();
        }

        //Reset the state back to idle
        numRootsLeft = 0;
        listeningState = ListeningState.IDLE;
    }

    public static void completeFeatureRenderer(FeatureRenderer<?, ?> featureRenderer) {
        //Complete the feature renderer entry using our stored values
        List<ModelPart> parts = ImmutableList.copyOf(storedFeatureRendererModelParts);
        FEATURE_RENDERER_PARTS.put(featureRenderer, parts);
        //Reset the state
        storedFeatureRendererModelParts.clear();
    }

    /**
     * Get root model parts for an entity renderer
     */
    public static List<ModelPart> getEntityRoots(EntityRenderer<?> entityRenderer) {
        return MODEL_PARTS.getOrDefault(entityRenderer, List.of());
    }

    /**
     * Get root model parts for a feature renderer
     */
    public static List<ModelPart> getFeatureRoots(FeatureRenderer<?, ?> featureRenderer) {
        return FEATURE_RENDERER_PARTS.getOrDefault(featureRenderer, List.of());
    }

    /**
     * This tracks whether the next call to
     * EntityRendererFactory.Context.getPart()
     * is being waited for, and for what purpose.
     *
     * See mixin/render/vanilla/sort/EntityRenderersMixin for more
     * details on how this global mixin stuff is working.
     */
    public enum ListeningState {
        IDLE,
        MODEL,
        FEATURE
    }

}
