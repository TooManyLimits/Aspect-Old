package io.github.moonlightmaya.mixin;

import io.github.moonlightmaya.AspectMod;
import io.github.moonlightmaya.util.AspectMatrixStack;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public class PlayerEntityRendererTestMixin {

    @Inject(method = "render(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At("TAIL"))
    public void renderInject(AbstractClientPlayerEntity abstractClientPlayerEntity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci) {
        //The matrix stack passed into this function is in WORLD space,
        //TRANSLATED relative to the player's feet!
        //**This matrix will transform from that space into camera space.** Keep this fact in mind when writing math and render code.
        AspectMod.updateTestAspect();
        AspectMod.TEST_ASPECT.renderEntity(vertexConsumerProvider, new AspectMatrixStack(matrixStack));
    }
}
