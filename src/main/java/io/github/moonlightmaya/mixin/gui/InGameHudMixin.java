package io.github.moonlightmaya.mixin.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.moonlightmaya.Aspect;
import io.github.moonlightmaya.manage.AspectManager;
import io.github.moonlightmaya.util.AspectMatrixStack;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class InGameHudMixin {

    //When rendering hud, render the hud parts of the camera entity, and
    //also the equipped GUI entity
    @Inject(method = "render", at = @At("HEAD"))
    public void renderStart(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        Entity cameraEntity = MinecraftClient.getInstance().getCameraEntity();
        AspectMatrixStack matrixStack = new AspectMatrixStack(matrices);

        //Draw aspect's hud parts if it exists
        if (cameraEntity != null) {
            Aspect cameraEntityAspect = AspectManager.getAspect(cameraEntity.getUuid());
            if (cameraEntityAspect != null)
                drawAspect(cameraEntityAspect, matrixStack);
        }

        //Draw gui aspect's hud parts if it exists
        if (AspectManager.getGuiAspect() != null) {
            drawAspect(AspectManager.getGuiAspect(), matrixStack);
        }
    }

    /**
     * Draw an aspect using hud rendering techniques
     */
    private static void drawAspect(Aspect aspect, AspectMatrixStack matrixStack) {
        VertexConsumerProvider.Immediate vcp = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();
        matrixStack.push();
        matrixStack.scale(-16, -16, -16);
        RenderSystem.disableDepthTest();
        DiffuseLighting.disableGuiDepthLighting();
        aspect.renderHud(vcp, matrixStack);
        vcp.draw();
        DiffuseLighting.enableGuiDepthLighting();
        RenderSystem.enableDepthTest();
        matrixStack.pop();
    }

}
