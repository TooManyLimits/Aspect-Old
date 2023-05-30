package io.github.moonlightmaya.mixin.render.vanilla.part;

import io.github.moonlightmaya.script.vanilla.VanillaPart;
import io.github.moonlightmaya.script.vanilla.VanillaRenderer;
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

    @Shadow public float pitch;
    @Shadow public float yaw;
    @Shadow public float pivotX;
    @Shadow public float pivotY;
    @Shadow public boolean visible;

    @Shadow public abstract void setAngles(float pitch, float yaw, float roll);

    //reuse variables to not allocate more than necessary
    private static final Matrix3f aspect$tempMatrix = new Matrix3f();
    private static final Matrix4f aspect$tempMatrix2 = new Matrix4f();
    private static final MatrixStack aspect$helperStack = new MatrixStack();
    private boolean aspect$savedVisibility;

    /**
     * At the very beginning of the method, set visibility to true.
     * This is done to bypass the return statement at the beginning, and ensure that
     * part matrices are updated unconditionally.
     */
    @Inject(method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;IIFFFF)V",
            at = @At("HEAD"))
    public void atBeginning(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, float red, float green, float blue, float alpha, CallbackInfo ci) {
        aspect$savedVisibility = visible;
        visible = true;
    }

    /**
     * Before the call to rotate(), take stock of the situation.
     * Grab the matrix before the part was transformed, and after the
     * part was transformed.
     *
     * The reason that we grab the post-transformation matrix *before* the
     * call to rotate is because MC's rotate() operates in a different space
     * than that of Aspects. The values of certain axes are negated, and
     * translations are applied, which complicates matters. The call to
     * loadCurrentTransformToHelperStack() is what applies the *real* transformations,
     * as Aspect would see them. If we injected after the call to rotate(), not only
     * would we need to apply Aspect's version of the rotation method, we'd also need
     * to reverse the vanilla one first.
     *
     * We also save this matrix on a stack. In case this part has any children, we want
     * their transforms to be calculated relative to their parent, not relative to the
     * global entity transform.
     */
    @Inject(method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;IIFFFF)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/ModelPart;rotate(Lnet/minecraft/client/util/math/MatrixStack;)V", shift = At.Shift.BEFORE))
    public void beforeRotate(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, float red, float green, float blue, float alpha, CallbackInfo ci) {
        //Reset visibility, we made it past the return statement, so we can set it back without fear
        //of the call being canceled
        visible = aspect$savedVisibility;

        //Guard this all inside the if statement. If there's no aspect currently being rendered,
        //Don't bother doing anything to the model part.
        if (!VanillaRenderer.CURRENT_RENDERER.isEmpty()) {
            VanillaRenderer topRenderer = VanillaRenderer.CURRENT_RENDERER.peek();

            //If there's no vanilla part registered for this, then just skip
            VanillaPart vanillaPart = topRenderer.vanillaPartInverse.get(this);

            if (vanillaPart != null) {

                //Save the visibility in the vanilla part
                vanillaPart.savedVisibility = aspect$savedVisibility;

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
     * applies its own transform to the matrix stack, we now
     * apply any customizations that an Aspect has made.
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
                vanillaPart.recalculateMatrixIfNeeded();
                aspect$tempMatrix2.set(vanillaPart.positionMatrix);
                matrices.multiplyPositionMatrix(aspect$tempMatrix2);
                aspect$tempMatrix.set(aspect$tempMatrix2).normal();
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
     * We forced the part's visibility to true unconditionally earlier,
     * but now we may want it to respect vanilla's desire to have the part
     * hidden. Here, we can cancel the call to render cuboids if the part
     * is supposed to be hidden.
     */
    @Inject(method = "renderCuboids", at = @At("HEAD"), cancellable = true)
    public void beforeRenderCuboids(MatrixStack.Entry entry, VertexConsumer vertexConsumer, int light, int overlay, float red, float green, float blue, float alpha, CallbackInfo ci) {
        if (!VanillaRenderer.CURRENT_RENDERER.isEmpty()) {
            VanillaRenderer topRenderer = VanillaRenderer.CURRENT_RENDERER.peek();
            VanillaPart vanillaPart = topRenderer.vanillaPartInverse.get(this);
            if (vanillaPart != null) {
                //If the part is set to invisible through petpet, or if Minecraft
                //wanted it invisible, cancel the rendering before the cubes have
                //a chance to be pushed.
                if (!vanillaPart.visible || !aspect$savedVisibility)
                    ci.cancel();
            }
        }
        //even if there isn't an aspect rendering currently, or there's no vanilla part, we need to remember that
        //if the visibility were set to false, then it should never have begun rendering here in the first place.
        //Cancel if that was the case
        if (!aspect$savedVisibility) ci.cancel();
    }

    /**
     * Loads the current transform into the helper stack, but with
     * the pitch and yaw inverted, and x and y pivot negated,
     * because yeah
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
