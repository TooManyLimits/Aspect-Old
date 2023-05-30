package io.github.moonlightmaya.script.vanilla;

import com.google.common.collect.ImmutableList;
import io.github.moonlightmaya.mixin.render.entity.LivingEntityRendererAccessor;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.ZombieBaseEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.HeadFeatureRenderer;
import net.minecraft.entity.EntityType;
import org.apache.commons.compress.utils.Lists;
import org.apache.logging.log4j.util.TriConsumer;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

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
    private static final Map<EntityRenderer<?>, List<ModelPart>> MODEL_PARTS = new HashMap<>();
    private static final Map<FeatureRenderer<?, ?>, List<ModelPart>> FEATURE_RENDERER_PARTS = new HashMap<>();

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
     * No matter how hard we try, certain entity renderers refuse to conform to a pattern. In vanilla, the only ones
     * we've found that require such intrusive changes are the Zombie and its derivatives, the Husk and Drowned.
     * We were unable to find a satisfactory way to deal with these automatically, and thus this SPECIAL_CASE_HANDLERS
     * map was born.
     *
     * The TriConsumer related to your entity renderer's class will be invoked when the EntityRenderer is complete()d.
     * If the class is not directly in the list, it will search superclasses - hence the HuskEntityRenderer will still
     * invoke the ZombieBaseEntityRenderer's function. Only one function will be invoked, the one for the class lowest
     * down the hierarchy.
     *
     * What exactly does the zombie do that breaks the pattern and requires this special case?
     * Well, when Aspect auto-generates feature renderers, it expects a certain ordering on the creation of ModelParts,
     * and their use in FeatureRenderers. Specifically:
     * - When a model part is created (tracked through EntityModelLoader.getModelPart()), it is added to a list.
     * - When addFeature() is called on a feature renderer, all parts in the list are viewed as "connected" to that
     *   feature renderer, and the list is cleared.
     * The trouble occurs when an EntityRenderer does the following:
     * - Non-feature-renderer model parts are created, unimportant for this example
     * - Create ModelPart A
     * - Create ModelPart B
     * - Use ModelPart B to create Feature Renderer 1
     * - Use ModelPart A to create Feature Renderer 2
     * In this case, the automatic code would assume that model parts A and B are both meant for
     * Feature Renderer 1. It has no way of knowing which model parts are for which feature renderers
     * aside from the order of execution. Zombies and their derivatives are the only mobs I know of that
     * deviate from this pattern: creating a model part, then creating an (unrelated) feature renderer,
     * then using that original model part later.
     *
     * To use this map, input your entity's EntityRenderer class as the key, and your function as the output.
     * The function accepts the instance of the entity renderer as well as the MODEL_PARTS and FEATURE_RENDERER_PARTS
     * maps in this class. Here, you may manipulate the parts as you see fit, according to the entity renderer.
     * This function is invoked after adding the entity renderer to the map.
     */
    public static final Map<Class<? extends EntityRenderer>, TriConsumer<EntityRenderer<?>, Map<EntityRenderer<?>, List<ModelPart>>, Map<FeatureRenderer<?, ?>, List<ModelPart>>>> SPECIAL_CASE_HANDLERS = new HashMap<>() {{
        put(ZombieBaseEntityRenderer.class, (renderer, entityRendererMap, featureRendererMap) -> {
            //The zombie's features are not perfectly detected by the automatic code. We need to manually fix them.
            ZombieBaseEntityRenderer instance = (ZombieBaseEntityRenderer) renderer;
            List<FeatureRenderer<?, ?>> instanceFeatures = ((LivingEntityRendererAccessor) instance).getFeatures();

            //For zombies, the armor feature is what is incorrect - the HeadFeatureRenderer has stolen its two model parts. Let's grab them:
            //The head feature renderer is the first feature renderer in the ZombieEntityRenderer.
            FeatureRenderer<?, ?> headFeature = instanceFeatures.get(0);
            //The armor feature renderer is the last (fourth) one.
            FeatureRenderer<?, ?> armorFeature = instanceFeatures.get(3);

            //Get the part lists for each head feature. It's supposed to be 7 for headFeature
            //and 2 for armorFeature, but the head feature has stolen the model parts from the armor feature,
            //meaning its 9 and 0.
            //Take those two stolen model parts away and send them back to the armor feature renderer.
            List<ModelPart> headFeatureParts = featureRendererMap.get(headFeature);
            List<ModelPart> armorFeatureParts = featureRendererMap.get(armorFeature);

            //Send the 2 parts over.
            armorFeatureParts.add(headFeatureParts.remove(0));
            armorFeatureParts.add(headFeatureParts.remove(0));
        });
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

            //Execute the special case if needed:
            Class<?> curClass = renderer.getClass();
            while (curClass != null) {
                if (SPECIAL_CASE_HANDLERS.containsKey(curClass)) {
                    SPECIAL_CASE_HANDLERS.get(curClass).accept(renderer, MODEL_PARTS, FEATURE_RENDERER_PARTS);
                    break;
                }
                curClass = curClass.getSuperclass();
            }
        }

        //Reset the state back to idle
        numRootsLeft = 0;
        listeningState = ListeningState.IDLE;
    }

    public static void completeFeatureRenderer(FeatureRenderer<?, ?> featureRenderer) {
        //Complete the feature renderer entry using our stored values (copied)
        List<ModelPart> parts = Lists.newArrayList(storedFeatureRendererModelParts.iterator());
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
