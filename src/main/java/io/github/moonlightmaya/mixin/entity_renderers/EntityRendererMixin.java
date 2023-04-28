package io.github.moonlightmaya.mixin.entity_renderers;

import io.github.moonlightmaya.Aspect;
import io.github.moonlightmaya.manage.AspectManager;
import io.github.moonlightmaya.util.AspectMatrixStack;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public class EntityRendererMixin {

    @Inject(method = "render", at = @At("HEAD"))
    public void onRender(Entity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        //The matrix stack passed into this function is in WORLD space,
        //TRANSLATED relative to the player's feet!
        //**This matrix will transform from that space into camera space.** Keep this fact in mind when writing math and render code.

        Aspect aspect = AspectManager.getAspect(entity.getUuid());
        if (aspect != null) {
            aspect.renderEntity(vertexConsumers, tickDelta, new AspectMatrixStack(matrices), light);
        }
    }

}
