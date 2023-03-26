package io.github.moonlightmaya.mixin;

import io.github.moonlightmaya.vanilla.VanillaPart;
import io.github.moonlightmaya.vanilla.VanillaRenderer;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelPart.class)
public abstract class ModelPartMixin {

    @Shadow public abstract void rotate(MatrixStack matrices);

    @Shadow public float roll;

    @Shadow public float pitch;
    @Shadow public float yaw;
    @Shadow public float pivotX;
    @Shadow public float pivotY;
    //reuse variables to not allocate more than necessary
    private final Matrix3f aspect$tempMatrix = new Matrix3f();

    private final MatrixStack aspect$helperStack = new MatrixStack();

    @Inject(method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;IIFFFF)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/ModelPart;rotate(Lnet/minecraft/client/util/math/MatrixStack;)V", shift = At.Shift.BEFORE))
    public void beforeRotate(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, float red, float green, float blue, float alpha, CallbackInfo ci) {
//        System.out.println("Pre rotate: \n" + matrices.peek().getPositionMatrix());

        //Guard this all inside the if statement. If there's no aspect currently being rendered,
        //Don't bother doing anything to the model part.
        if (!VanillaRenderer.CURRENT_RENDERER.isEmpty()) {
            VanillaRenderer topRenderer = VanillaRenderer.CURRENT_RENDERER.peek();

            //If there's no vanilla part registered for this, then just skip
            VanillaPart vanillaPart = topRenderer.vanillaPartInverse.get(this);

            if (vanillaPart != null) {

                Matrix4f preRenderTotal = new Matrix4f(topRenderer.savedVanillaModelTransform);
                Matrix4f midRenderTotal = matrices.peek().getPositionMatrix();
                aspect$helperStack.loadIdentity();
//                roll = -roll;
                yaw = -yaw;
                pitch = -pitch;
                float savedPivotX = pivotX;
                float savedPivotY = pivotY;
                pivotX = pivotY = 0;
                rotate(aspect$helperStack);
                pivotX = savedPivotX;
                pivotY = savedPivotY;
                pitch = -pitch;
                yaw = -yaw;
//                roll = -roll;
//                System.out.println("Post rotate: \n" + postRotationTotal);

                //Grab the matrix from the current vanilla renderer
                //Cancel it out with the current matrix to calculate
                //the matrix applied to this part specifically
                Matrix4f correctedTotalTransform = preRenderTotal
                        .invert()
                        .mul(midRenderTotal)
                        .mul(aspect$helperStack.peek().getPositionMatrix());


                //correctedTotalTransform now contains the transformation of this specific model part
                //relative to the entire entity's transform, so we'll save that
                //in the corresponding vanilla part
                vanillaPart.savedTransform.set(correctedTotalTransform);
            }
        }

    }

    /**
     * Just after the rotate() call, where the model part
     * applies its own transform to the matrix stack, we
     * inject here to compare this matrix against the one
     * that was previously set inside
     * LivingEntityRendererMixin.saveModelTransform().
     */
    @Inject(method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;IIFFFF)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/ModelPart;rotate(Lnet/minecraft/client/util/math/MatrixStack;)V", shift = At.Shift.AFTER))
    public void afterRotate(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, float red, float green, float blue, float alpha, CallbackInfo ci) {
        //Guard this all inside the if statement. If there's no aspect currently being rendered,
        //Don't bother doing anything to the model part.
        if (!VanillaRenderer.CURRENT_RENDERER.isEmpty()) {
            VanillaRenderer topRenderer = VanillaRenderer.CURRENT_RENDERER.peek();
            //If there's no vanilla part registered for this, then just skip
            VanillaPart vanillaPart = topRenderer.vanillaPartInverse.get(this);
            if (vanillaPart != null) {
                //Now that we've saved the thing applied by vanilla,
                //we'll apply Aspect's modification matrix on top of it.
                matrices.multiplyPositionMatrix(vanillaPart.appliedTransform);
                aspect$tempMatrix.set(vanillaPart.appliedTransform).normal();
                matrices.peek().getNormalMatrix().mul(aspect$tempMatrix);
            }
        }
    }


}
