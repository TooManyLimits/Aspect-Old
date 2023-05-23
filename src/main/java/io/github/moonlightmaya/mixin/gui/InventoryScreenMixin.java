package io.github.moonlightmaya.mixin.gui;

import io.github.moonlightmaya.Aspect;
import io.github.moonlightmaya.manage.AspectManager;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public class InventoryScreenMixin {

    /**
     * When we render an aspect inside an inventory, set the context
     * accordingly so people's scripts can detect it
     */
    @Inject(method = "drawEntity(Lnet/minecraft/client/util/math/MatrixStack;IIILorg/joml/Quaternionf;Lorg/joml/Quaternionf;Lnet/minecraft/entity/LivingEntity;)V", at = @At("HEAD"))
    private static void setRenderContextInsideInventory(MatrixStack matrices, int x, int y, int size, Quaternionf quaternionf, Quaternionf quaternionf2, LivingEntity entity, CallbackInfo ci) {
        Aspect aspect = AspectManager.getAspect(entity.getUuid());
        if (aspect != null)
            aspect.renderContext = Aspect.RenderContexts.MINECRAFT_GUI;
    }

}
