package io.github.moonlightmaya.mixin.render.vanilla.sort;

import com.google.common.collect.ImmutableMap;
import io.github.moonlightmaya.script.vanilla.EntityRendererMaps;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.EntityRenderers;
import net.minecraft.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * EntityRenderers.class deals with the various different entity models
 * in the game, and reloading them when needed.
 */
@Mixin(EntityRenderers.class)
public class EntityRenderersMixin {

    /**
     * Inject before the call to factory.create(context)
     * This is the call which constructs the instance of an EntityRenderer
     * (Because the constructor of most entity renderers implements EntityRendererFactory)
     *
     * After this call is made, the next call to EntityRendererFactory.Context.getPart()
     * will generally be the model of the entity whose renderer we're currently creating.
     *
     * getPart() may be called multiple times, but except in a few circumstances, any
     * subsequent calls after the first are for * feature renderers *, not the main model.
     * The special circumstances are:
     * - Pufferfish, which store all 3 puffed versions of the model (small, medium, large)
     * - Tropical fish, which store both "large" and "small" variants of the model
     *
     * In some cases getPart() might not be called at all, so we should keep that in mind
     * and provide an empty list of model parts.
     */
    @Inject(method = "method_32174", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/entity/EntityRendererFactory;create(Lnet/minecraft/client/render/entity/EntityRendererFactory$Context;)Lnet/minecraft/client/render/entity/EntityRenderer;",
            shift = At.Shift.BEFORE
    ))
    private static void reloadEntityRenderersMixin(ImmutableMap.Builder<EntityType<?>, EntityRendererFactory<?>> builder, EntityRendererFactory.Context context, EntityType<?> entityType, EntityRendererFactory<?> factory, CallbackInfo ci) {
        EntityRendererMaps.prime();
    }

    /**
     * The same thing needs to be done when building the player renderers.
     */
    @Inject(method = "method_32175", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/entity/EntityRendererFactory;create(Lnet/minecraft/client/render/entity/EntityRendererFactory$Context;)Lnet/minecraft/client/render/entity/EntityRenderer;",
            shift = At.Shift.BEFORE
    ))
    private static void reloadPlayerRenderersMixin(ImmutableMap.Builder<EntityType<?>, EntityRendererFactory<?>> builder, EntityRendererFactory.Context context, String modelType, EntityRendererFactory<?> factory, CallbackInfo ci) {
        EntityRendererMaps.prime();
    }

    /**
     *
     *
     * After the entity renderer is done being created, end the collection.
     * Also, grab the instance of the entity renderer that was just created.
     * This is why we have a ModifyArg, to view the output.
     *
     *
     */

    @ModifyArg(
            method = "method_32174",
            at = @At(
                value = "INVOKE",
                target = "Lcom/google/common/collect/ImmutableMap$Builder;put(Ljava/lang/Object;Ljava/lang/Object;)Lcom/google/common/collect/ImmutableMap$Builder;"
            ),
            index = 1
    )
    private static Object captureEntityRenderer(Object v) {
        EntityRendererMaps.complete((EntityRenderer<?>) v);
        return v;
    }

    @ModifyArg(
            method = "method_32175",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/google/common/collect/ImmutableMap$Builder;put(Ljava/lang/Object;Ljava/lang/Object;)Lcom/google/common/collect/ImmutableMap$Builder;"
            ),
            index = 1
    )
    private static Object capturePlayerRenderer(Object v) {
        EntityRendererMaps.complete((EntityRenderer<?>) v);
        return v;
    }

}
