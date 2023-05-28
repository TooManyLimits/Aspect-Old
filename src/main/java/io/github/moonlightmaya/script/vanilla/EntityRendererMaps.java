package io.github.moonlightmaya.script.vanilla;

import io.github.moonlightmaya.util.RenderUtils;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.entity.Entity;
import org.jetbrains.annotations.Nullable;
import petpet.types.PetPetTable;

import java.util.HashMap;
import java.util.Map;

/**
 * This class deals with the various global state of vanilla model parts
 * and entity renderers. It keeps track of mapping from EntityRenderer
 * instances to their associated vanilla model part trees.
 */
public class EntityRendererMaps {


    private static ListeningState listeningState;

    private static ModelPart storedModelPart = null;

    /**
     * The current set of vanilla model parts, indexed
     * by their EntityRenderer. Modified by various mixins
     * when needed.
     */
    private static final Map<EntityRenderer<?>, @Nullable ModelPart> MODEL_PARTS = new HashMap<>();

    /**
     * Clear out the maps. Used when the game is reloaded
     * and the entity renderer instances change.
     */
    public static void clear() {
        MODEL_PARTS.clear();
        storedModelPart = null;
        listeningState = ListeningState.IDLE;
    }

    /**
     * Primes the entity renderer maps, awaiting new getPart() calls
     */
    public static void prime() {
        listeningState = ListeningState.MODEL;
    }

    public static void onPartCreated(ModelPart part) {
        switch (listeningState) {
            case IDLE -> {}
            case MODEL -> {
                //Save this model part, later when we complete() the renderer
                //we will save it in there
                storedModelPart = part;

                //We got the model, start looking for features now
                listeningState = ListeningState.FEATURE;
            }
            case FEATURE -> {
                //TODO
            }
        }
    }

    public static void complete(EntityRenderer<?> renderer) {
        //Complete the renderer entry using our stored values
        if (storedModelPart != null) {
            MODEL_PARTS.put(renderer, storedModelPart);
            storedModelPart = null;
        }

        //Reset the state back to idle
        listeningState = ListeningState.IDLE;
    }

    public static @Nullable ModelPart getRoot(Entity entity) {
        EntityRenderer<?> renderer = RenderUtils.getRenderer(entity);
        return MODEL_PARTS.get(renderer);
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
