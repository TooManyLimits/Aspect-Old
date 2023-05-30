package io.github.moonlightmaya.mixin.render.entity;

import io.github.moonlightmaya.script.vanilla.EntityRendererMaps;
import io.github.moonlightmaya.script.vanilla.VanillaFeature;
import io.github.moonlightmaya.script.vanilla.VanillaRenderer;
import io.github.moonlightmaya.util.RenderUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Iterator;
import java.util.List;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {

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
            //This matrix goes from entity space -> view space
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

    /**
     *
     *
     * HANDLING FOR FEATURE RENDERERS
     *
     *
     */

    @Redirect(
            method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/entity/feature/FeatureRenderer;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/entity/Entity;FFFFFF)V"
            )
    )
    public void alterFeatureRenderer(FeatureRenderer featureRenderer, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int light, Entity entity, float v0, float v1, float v2, float v3, float v4, float v5) {
        if (!VanillaRenderer.CURRENT_RENDERER.isEmpty()) {
            //Get the VanillaFeature corresponding to this feature renderer:
            VanillaRenderer topRenderer = VanillaRenderer.CURRENT_RENDERER.peek();
            VanillaFeature vanillaFeature = topRenderer.featureRendererInverse.get(featureRenderer);
            if (vanillaFeature != null) {
                if (!vanillaFeature.visible) {
                    //If invisible, render the original with our silly little VCP so that no vertices actually appear,
                    //but all the operations of rendering still take place.
                    featureRenderer.render(matrixStack, RenderUtils.getSillyLittleVcp(), light, entity, v0, v1, v2, v3, v4, v5);
                } else {
                    //Otherwise, apply our own fancy modifications to the matrices:
                    vanillaFeature.recalculateMatrixIfNeeded();
                    matrixStack.push();
                    matrixStack.multiplyPositionMatrix(vanillaFeature.positionMatrix);
                    //And render as usual
                    featureRenderer.render(matrixStack, vertexConsumerProvider, light, entity, v0, v1, v2, v3, v4, v5);
                    matrixStack.pop(); //Remove our transformation
                }
                return;
            }
        }
        //If nothing out of the order happened (if it did, that would cause an early return), then render normally
        featureRenderer.render(matrixStack, vertexConsumerProvider, light, entity, v0, v1, v2, v3, v4, v5);
    }

    @Inject(method = "addFeature", at = @At(value = "HEAD"))
    public void completeFeatureRenderer(FeatureRenderer<?, ?> feature, CallbackInfoReturnable<Boolean> cir) {
        EntityRendererMaps.completeFeatureRenderer(feature);
    }


}
