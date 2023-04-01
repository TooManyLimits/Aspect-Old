package io.github.moonlightmaya.mixin;

import io.github.moonlightmaya.vanilla.VanillaPart;
import io.github.moonlightmaya.vanilla.VanillaRenderer;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(ModelPart.class)
public abstract class ModelPartMixin {

    @Shadow public abstract void rotate(MatrixStack matrices);

    @Shadow public float pitch;
    @Shadow public float yaw;
    @Shadow public float roll;
    @Shadow public float pivotZ;
    @Shadow public float pivotX;
    @Shadow public float pivotY;
    //reuse variables to not allocate more than necessary
    private static final Matrix3f aspect$tempMatrix = new Matrix3f();
    private static final Matrix4f aspect$tempMatrix2 = new Matrix4f();
    private static final MatrixStack aspect$helperStack = new MatrixStack();

    @Inject(method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;IIFFFF)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/ModelPart;rotate(Lnet/minecraft/client/util/math/MatrixStack;)V", shift = At.Shift.BEFORE))
    public void beforeRotate(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, float red, float green, float blue, float alpha, CallbackInfo ci) {

        //Guard this all inside the if statement. If there's no aspect currently being rendered,
        //Don't bother doing anything to the model part.
        if (!VanillaRenderer.CURRENT_RENDERER.isEmpty()) {
            VanillaRenderer topRenderer = VanillaRenderer.CURRENT_RENDERER.peek();

            //If there's no vanilla part registered for this, then just skip
            VanillaPart vanillaPart = topRenderer.vanillaPartInverse.get(this);

            if (vanillaPart != null) {

                //Get the matrices before rendering this model part, and
                //the matrices after, then cancel them out to get the
                //net change.

                //Matrices before this model part depend on the state of the model
                //part children helper. If it's empty, this is a root, so we use
                //topRenderer.savedVanillaModelTransform, otherwise, we peek
                //the stack. Store the result into tempMatrix2.
                if (topRenderer.modelPartsChildrenHelper.isEmpty()) {
                    aspect$tempMatrix2.set(topRenderer.savedVanillaModelTransform);
                } else {
                    topRenderer.modelPartsChildrenHelper.peekInto(aspect$tempMatrix2);
                }

                Matrix4f midRenderTotal = matrices.peek().getPositionMatrix();

                //Load the current transformation into the helper stack :P
                loadCurrentTransformToHelperStack();

                //Cancel out the pre-render matrix with the current one to calculate
                //the matrix applied to this part specifically
                Matrix4f correctedTotalTransform = aspect$tempMatrix2
                        .invert()
                        .mul(midRenderTotal)
                        .mul(aspect$helperStack.peek().getPositionMatrix());

                //correctedTotalTransform now contains the transformation of this specific model part
                //relative to the entire entity's transform, so we'll save that
                //in the corresponding vanilla part
                vanillaPart.savedTransform.set(correctedTotalTransform);

                topRenderer.modelPartsChildrenHelper.push(correctedTotalTransform);
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

    @Inject(method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;IIFFFF)V",
    at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;pop()V", shift = At.Shift.BEFORE))
    public void afterDrawChildren(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, float red, float green, float blue, float alpha, CallbackInfo ci) {
        if (!VanillaRenderer.CURRENT_RENDERER.isEmpty()) {
            VanillaRenderer topRenderer = VanillaRenderer.CURRENT_RENDERER.peek();
            VanillaPart vanillaPart = topRenderer.vanillaPartInverse.get(this);
            if (vanillaPart != null) {
                topRenderer.modelPartsChildrenHelper.pop(); //Undo the push from earlier
            }
        }
    }

    /**
     * Loads the current transform into the helper stack, but with
     * the pitch and yaw inverted, because yeah
     */
    private void loadCurrentTransformToHelperStack() {
        aspect$helperStack.loadIdentity();
        yaw = -yaw;
        pitch = -pitch;
        pivotX = -pivotX;
        pivotY = -pivotY;
        rotate(aspect$helperStack);
        pivotY = -pivotY;
        pivotX = -pivotX;
        pitch = -pitch;
        yaw = -yaw;
    }

}
