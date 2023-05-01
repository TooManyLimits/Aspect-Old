package io.github.moonlightmaya.model.rendertasks;

import io.github.moonlightmaya.model.Transformable;
import io.github.moonlightmaya.util.AspectMatrixStack;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;

/**
 * Represents an action that is run every frame,
 * that renders some kind of object in space.
 * This can be an item, block, text, or even an
 * entity. This superclass handles the transformation
 * of the part and setting up matrices, while subclasses
 * deal with the rendering itself.
 */
public abstract class RenderTask extends Transformable {

    protected static final MatrixStack VANILLA_STACK = new MatrixStack();

    public void render(AspectMatrixStack matrixStack, VertexConsumerProvider vcp, int light, int overlay) {
        if (visible) {
            matrixStack.push();
            this.recalculateMatrixIfNeeded();
            matrixStack.multiply(this.positionMatrix, this.normalMatrix);
            matrixStack.fillVanilla(VANILLA_STACK);
            matrixStack.pop();
            specializedRender(vcp, light, overlay);
        }
    }

    protected abstract void specializedRender(VertexConsumerProvider vcp, int light, int overlay);

}
