package io.github.moonlightmaya.mixin.entity_renderers;

import io.github.moonlightmaya.vanilla.VanillaEntityInfoTable;
import io.github.moonlightmaya.vanilla.VanillaRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {

    @Shadow
    protected abstract void scale(LivingEntity entity, MatrixStack matrices, float amount);

    private final Matrix4f preTransformSnapshot = new Matrix4f();
    private final MatrixStack helperStack = new MatrixStack();
    private final Vector3f scaleVec = new Vector3f();

    @Inject(method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("HEAD"))
    public void beginRender(LivingEntity livingEntity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci) {
        //When we begin rendering, take a snapshot of the top of the matrix stack.
        preTransformSnapshot.set(matrixStack.peek().getPositionMatrix());
    }

    @Inject(method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/client/render/entity/LivingEntityRenderer;getRenderLayer(Lnet/minecraft/entity/LivingEntity;ZZZ)Lnet/minecraft/client/render/RenderLayer;"))
    public void saveModelTransform(LivingEntity livingEntity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci) {
        //Only run this code when the given entity is being rendered with an aspect.
        if (!VanillaRenderer.CURRENT_RENDERER.empty()) {

            //Right after the getRenderLayer() call, we take another glance at the top of the matrix stack.
            //Compare the matrix with the pre-transform snapshot we took earlier, and cancel things out,
            //to obtain the total transformation matrix applied by the LivingEntityRenderer.
            preTransformSnapshot.invert().mul(matrixStack.peek().getPositionMatrix());

            //Transformed now contains the transformation matrix that was applied by the LivingEntityRenderer.
            //However, we now have to undo the flip that Minecraft did, as well as the extra translation.
            //Grab the scale factor:
            helperStack.loadIdentity();
            this.scale(livingEntity, helperStack, g);
            Matrix4f pos = helperStack.peek().getPositionMatrix();
            scaleVec.set(pos.get(0, 0), pos.get(1, 1), pos.get(2, 2));

            preTransformSnapshot.translate(0, 1.501f, 0);
            preTransformSnapshot.scale(-1f, -1f, 1f);

            //Save this matrix in the currently set vanilla renderer.
            VanillaRenderer.CURRENT_RENDERER.peek().modelTransform.set(preTransformSnapshot);
        }

    }

}
