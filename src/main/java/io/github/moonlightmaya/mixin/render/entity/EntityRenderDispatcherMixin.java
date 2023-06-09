package io.github.moonlightmaya.mixin.render.entity;

import io.github.moonlightmaya.Aspect;
import io.github.moonlightmaya.manage.AspectManager;
import io.github.moonlightmaya.vanilla.EntityRendererMaps;
import io.github.moonlightmaya.vanilla.VanillaRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {

    //Mixin plugin says this method header is wrong but uhhh it still runs :skull:
    @Inject(method = "render", at = @At(value = "INVOKE", shift = At.Shift.BEFORE,
            target = "Lnet/minecraft/client/render/entity/EntityRenderer;render(Lnet/minecraft/entity/Entity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"),
            locals = LocalCapture.CAPTURE_FAILEXCEPTION)
    public void beforeRenderEntity(Entity entity, double x, double y, double z, float yaw,
                                   float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                                   int light, CallbackInfo ci,
                                   EntityRenderer renderer, Vec3d offset,
                                   double unused2, double unused3, double unused4) {
        Aspect aspect = AspectManager.getAspect(entity.getUuid());
        if (aspect != null) {
            VanillaRenderer.CURRENT_RENDERER.push(aspect.vanillaRenderer);
            //Update the vanilla renderer if needed
            if (aspect.vanillaRenderer.needsUpdate)
                aspect.vanillaRenderer.update(entity);

            // Save the current entity -> view matrix if needed.
            // This matrix is also calculated in LivingEntityRendererMixin, but
            // if this isn't a living entity, that will never be called, so
            // we need to do it here instead, before the render occurs.
            if (!(renderer instanceof LivingEntityRenderer<?,?>))
                aspect.vanillaRenderer.savedVanillaModelTransform.set(matrices.peek().getPositionMatrix());
            //Just saving this render offset in case someone wants to access it, even though it's not used in rendering
            aspect.vanillaRenderer.renderOffset.set(offset.x, offset.y, offset.z);
        }

    }

    @Inject(method = "render", at = @At(value = "INVOKE", shift = At.Shift.AFTER,
            target = "Lnet/minecraft/client/render/entity/EntityRenderer;render(Lnet/minecraft/entity/Entity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"))
    public void afterRenderEntity(Entity entity, double x, double y, double z, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        Aspect aspect = AspectManager.getAspect(entity.getUuid());
        if (aspect != null)
            VanillaRenderer.CURRENT_RENDERER.pop();
    }


    /**
     * Clear the vanilla part maps when we reload, because
     * the instances of entity renderers change.
     * Also mark all vanilla renderers as needing to be updated.
     */
    @Inject(method = "reload", at = @At("HEAD"))
    public void clearVanillaPartMaps(ResourceManager manager, CallbackInfo ci) {
        EntityRendererMaps.clear();
        AspectManager.forEachAspect(aspect -> {
            //If the aspect vanilla renderer is initialized (read: now wrong),
            //mark it as needing an update
            if (aspect.vanillaRenderer.initialized)
                aspect.vanillaRenderer.needsUpdate = true;
        });
    }

}
