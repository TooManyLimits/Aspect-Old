package io.github.moonlightmaya.mixin;

import io.github.moonlightmaya.Aspect;
import io.github.moonlightmaya.AspectMod;
import io.github.moonlightmaya.manage.AspectManager;
import io.github.moonlightmaya.util.AspectMatrixStack;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {

    @Inject(method = "renderEntity", at = @At("TAIL"))
    public void renderWorldParts(Entity entity, double cameraX, double cameraY, double cameraZ, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, CallbackInfo ci) {

        Aspect aspect = AspectManager.getAspect(entity.getUuid());
        if (aspect != null) {
            AspectMatrixStack aspectMatrixStack = new AspectMatrixStack(matrices);
            aspectMatrixStack.translate(-cameraX, -cameraY, -cameraZ);

            aspect.renderEntity(vertexConsumers, aspectMatrixStack);
        }
        
    }

}
