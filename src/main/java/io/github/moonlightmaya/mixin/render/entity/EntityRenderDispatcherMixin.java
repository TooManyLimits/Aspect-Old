package io.github.moonlightmaya.mixin.render.entity;

import io.github.moonlightmaya.Aspect;
import io.github.moonlightmaya.manage.AspectManager;
import io.github.moonlightmaya.script.vanilla.EntityRendererMaps;
import io.github.moonlightmaya.script.vanilla.VanillaRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
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

    @Inject(method = "render", at = @At(value = "INVOKE", shift = At.Shift.BEFORE,
            target = "Lnet/minecraft/client/render/entity/EntityRenderer;render(Lnet/minecraft/entity/Entity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"),
            locals = LocalCapture.CAPTURE_FAILHARD)
    public void beforeRenderEntity(Entity entity, double x, double y, double z, float yaw,
                                   float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                                   int light, CallbackInfo ci,
                                   EntityRenderer unused, Vec3d offset,
                                   double unused2, double unused3, double unused4) {
        Aspect aspect = AspectManager.getAspect(entity.getUuid());
        if (aspect != null) {
            VanillaRenderer.CURRENT_RENDERER.push(aspect.vanillaRenderer);

            // Save the current entity -> view matrix if needed.
            // This matrix is also calculated in LivingEntityRendererMixin, but
            // if this isn't a living entity, that will never be called, so
            // we need to do it here instead, before the render occurs.
            if (!(entity instanceof LivingEntity))
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
     */
    @Inject(method = "reload", at = @At("HEAD"))
    public void clearVanillaPartMaps(ResourceManager manager, CallbackInfo ci) {
        EntityRendererMaps.clear();
    }

}
