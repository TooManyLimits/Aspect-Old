package io.github.moonlightmaya.mixin;

import io.github.moonlightmaya.Aspect;
import io.github.moonlightmaya.manage.AspectManager;
import io.github.moonlightmaya.vanilla.VanillaRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
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
            //Just saving this render offset in case someone wants to access it, even though it's not used in rendering
            aspect.vanillaRenderer.renderOffset.set((float) offset.x, (float) offset.y, (float) offset.z);
        }

    }

    @Inject(method = "render", at = @At(value = "INVOKE", shift = At.Shift.AFTER,
            target = "Lnet/minecraft/client/render/entity/EntityRenderer;render(Lnet/minecraft/entity/Entity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"))
    public void afterRenderEntity(Entity entity, double x, double y, double z, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        Aspect aspect = AspectManager.getAspect(entity.getUuid());
        if (aspect != null)
            VanillaRenderer.CURRENT_RENDERER.pop();
    }


}
