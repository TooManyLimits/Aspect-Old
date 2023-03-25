package io.github.moonlightmaya.mixin.entity_renderers;

import io.github.moonlightmaya.vanilla.VanillaRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin {

    private final Matrix4f preTransformSnapshot = new Matrix4f();
    private final Matrix4f transformed = new Matrix4f();

    @Inject(method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("HEAD"))
    public void beginRender(LivingEntity livingEntity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci) {
        //When we begin rendering, take a snapshot of the top of the matrix stack.
        preTransformSnapshot.set(matrixStack.peek().getPositionMatrix());
    }

    @Inject(method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/client/render/entity/LivingEntityRenderer;getRenderLayer(Lnet/minecraft/entity/LivingEntity;ZZZ)Lnet/minecraft/client/render/RenderLayer;"))
    public void saveModelTransform(LivingEntity livingEntity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci) {
        //Right after the getRenderLayer() call, we take another glance at the top of the matrix stack.
        transformed.set(matrixStack.peek().getPositionMatrix());
        //Compare the matrix with the pre-transform snapshot we took earlier, and cancel things out,
        //to obtain the total transformation matrix applied by the LivingEntityRenderer.
        preTransformSnapshot.mul(transformed.invert());
        //Transformed now contains the transformation matrix that was applied by the LivingEntityRenderer.
        //Save this matrix in the currently set vanilla renderer.
        if (!VanillaRenderer.CURRENT_RENDERER.empty())
            VanillaRenderer.CURRENT_RENDERER.peek().modelTransform.set(transformed);
    }

}
