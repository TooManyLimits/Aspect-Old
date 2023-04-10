package io.github.moonlightmaya.mixin.entity_renderers;

import io.github.moonlightmaya.vanilla.VanillaModelPartSorter;
import io.github.moonlightmaya.vanilla.VanillaRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import org.joml.Matrix3d;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {


    @Shadow protected EntityModel<?> model;
    private final Matrix4f aspect$preTransformSnapshot = new Matrix4f();
    private final Matrix3f aspect$tempMatrix = new Matrix3f();

    @Inject(method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("HEAD"))
    public void beginRender(LivingEntity livingEntity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci) {
        if (!VanillaRenderer.CURRENT_RENDERER.isEmpty()) {
            //When we begin rendering, take a snapshot of the top of the matrix stack.
            aspect$preTransformSnapshot.set(matrixStack.peek().getPositionMatrix());
        }
    }

    @Inject(method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/client/render/entity/LivingEntityRenderer;getRenderLayer(Lnet/minecraft/entity/LivingEntity;ZZZ)Lnet/minecraft/client/render/RenderLayer;"))
    public void saveModelTransform(LivingEntity livingEntity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci) {
        //Only run this code when the given entity is being rendered with an aspect.
        if (!VanillaRenderer.CURRENT_RENDERER.isEmpty()) {
            VanillaRenderer topRenderer = VanillaRenderer.CURRENT_RENDERER.peek();

            //Right after the getRenderLayer() call, we take another glance at the top of the matrix stack.
            //Compare the matrix with the pre-transform snapshot we took earlier, and cancel things out,
            //to obtain the total transformation matrix applied by the LivingEntityRenderer.
            aspect$preTransformSnapshot.invert().mul(matrixStack.peek().getPositionMatrix());

            //Transformed now contains the transformation matrix that was applied by the LivingEntityRenderer.
            //However, we now have to undo the x/y flip that Minecraft did, as well as the extra translation.

            //Save the original matrix before applying our wacky flip and translate to it
            topRenderer.savedVanillaModelTransform.set(matrixStack.peek().getPositionMatrix());

            //And apply it to the matrix, undoing the previous things applied by Minecraft
            //for some arcane reason, that i will not bother to understand. I seriously do not understand
            //this at all. But. This number *needs to be 1.5, not 1.501.* The number in minecraft's code
            //is 1.501. However. It needs to *not* be 1.501 here.
            aspect$preTransformSnapshot.translate(0, 1.500f, 0);
            aspect$preTransformSnapshot.scale(-1f, -1f, 1f);

            //Multiply in the applied transform, to both the snapshot and the vanilla matrix stack
            aspect$preTransformSnapshot.mul(topRenderer.appliedVanillaModelTransform);

            matrixStack.multiplyPositionMatrix(topRenderer.appliedVanillaModelTransform);
            aspect$tempMatrix.set(topRenderer.appliedVanillaModelTransform).normal();
            matrixStack.peek().getNormalMatrix().mul(aspect$tempMatrix);

            //Save this matrix in the currently set vanilla renderer, as its "aspectModelTransform".
            topRenderer.aspectModelTransform.set(aspect$preTransformSnapshot);


        }

    }

}
