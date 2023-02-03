package io.github.moonlightmaya;

import com.google.common.collect.ImmutableList;
import io.github.moonlightmaya.util.AspectMatrixStack;
import net.minecraft.client.render.*;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.joml.*;

import java.util.List;

/**
 * An element of a hierarchical tree structure, analogous to the structure in Blockbench as well as the file system.
 */
public class AspectModelPart {
    private String name;

    //following are temporarily public for testing!
    public List<AspectModelPart> children; //null if no children
    public final Matrix4f positionMatrix = new Matrix4f();
    public final Matrix3f normalMatrix = new Matrix3f();
    public final Vector3f partPivot = new Vector3f();
    public final Vector3f partPos = new Vector3f();
    public final Quaternionf partRot = new Quaternionf();
    public final Vector3f partScale = new Vector3f(1, 1, 1);

    //Whether this part needs its matrix recalculated. After calling rot(), pos(), etc. this will be set to true.
    //If it's true at rendering time, then the this.partMatrix field will be updated, and this will be set to false.
    public boolean needsMatrixRecalculation = true;
    public float[] vertexData; //null if no vertices in this part

    /**
     * Contains the render layers we wish to draw to
     * These may be vanilla render layers,
     * or layers for custom core shaders we add,
     * or even custom layers provided by an equipped Aspect.
     *
     * If this is null, then copy the render layers of the parent.
     */
    private @Nullable List<RenderLayer> renderLayers;

    /**
     * Returns whether this part has vertex data attached or not.
     */
    public boolean hasVertexData() {
        return vertexData != null;
    }

    /**
     * Returns whether this part has children or not.
     */
    public boolean hasChildren() {
        return children != null;
    }

    private void recalculateMatrixIfNecessary() {
        if (needsMatrixRecalculation) {
            //We want to scale, then rotate, then translate, so _of course_ we have to call these functions in the order
            //translate, rotate, scale! Because that's the convention, apparently... okay i guess
            positionMatrix.translation(partPivot)
                    .rotate(partRot)
                    .scale(partScale)
                    .translate(partPos)
                    .translate(partPivot.negate());
            partPivot.negate();
            //Compute the normal matrix as well and store it
            positionMatrix.normal(normalMatrix);
            //Matrices are now calculated, don't need to be recalculated anymore
            needsMatrixRecalculation = false;
        }
    }

    private static final List<RenderLayer> DEFAULT_LAYERS = ImmutableList.of(RenderLayer.getEntityCutoutNoCull(new Identifier("textures/entity/creeper/creeper.png"))); //aww man
    /**
     * Renders with the given VCP.
     * Sets up the initial call to the internal render function, which has many parameters
     * to use the call stack as a transformation/customization stack.
     * When rendering in compatibility mode, this is the regular VertexConsumerProvider.Immediate that MC uses
     * to immediate-mode-draw entities on the screen.
     * In optimized mode, this is a custom VCP that will save its buffers to VertexBuffer objects to use
     * our custom core shaders, allowing us to avoid re-uploading vertices each frame.
     *
     * Depending on whether we're in optimized or compatible mode, the provided matrixStack will also be
     * different.
     */
    public void render(VertexConsumerProvider vcp, AspectMatrixStack matrixStack) {
        List<RenderLayer> defaultLayers = DEFAULT_LAYERS;
        renderInternal(vcp, defaultLayers, matrixStack);
    }

    /**
     * Recursively renders this part and its children to the VCP
     * The part customization stack from Figura Rewrite II is removed, in favor of using
     * the call stack with parameters instead. (Just like prewrite did~ lol)
     * This time though, things are different.
     * Each parameter will be commented and documented, so that the organization
     * doesn't get out of hand!
     */
    private void renderInternal(
            VertexConsumerProvider vcp, //The VCP used for this rendering call, where we will fetch buffers from using the render layers.
            List<RenderLayer> currentRenderLayers, //The current set of render layers for the part. Inherited from the parent if this.renderLayers is null.
            AspectMatrixStack matrixStack //The current matrix stack.
    ) {
        //If this model part's layers are not null, then set the current ones to our overrides. Otherwise, keep the parent's render layers.
        if (this.renderLayers != null)
            currentRenderLayers = this.renderLayers;

        //Push and transform the stack, if necessary
        if (hasVertexData() || hasChildren()) {
            //Recalculate the matrices for this part if necessary, then apply them to the stack.
            recalculateMatrixIfNecessary();
            matrixStack.push();
            matrixStack.multiply(positionMatrix, normalMatrix);
        }

        if (hasVertexData()) {
            for (RenderLayer layer : currentRenderLayers) {
                //Obtain a vertex buffer from the VCP, then put all our vertices into it.
                VertexConsumer buffer = vcp.getBuffer(layer);
                for (int i = 0; i < vertexData.length; i += 8) {
                    Vector4f pos = new Vector4f(vertexData[i], vertexData[i+1], vertexData[i+2], 1f);
                    matrixStack.peekPosition().transform(pos);
                    Vector3f normal = new Vector3f(vertexData[i+5], vertexData[i+6], vertexData[i+7]);
                    matrixStack.peekNormal().transform(normal);
                    buffer.vertex(
                            pos.x, pos.y, pos.z, //Position
                            1f, 1f, 1f, 1f, //Color
                            vertexData[i+3], vertexData[i+4], //Texture
                            OverlayTexture.DEFAULT_UV, //"Overlay"
                            LightmapTextureManager.MAX_LIGHT_COORDINATE, //Light
                            normal.x, normal.y, normal.z //Normal
                    );
                }
            }
        }

        //If there are children, render them all too
        if (hasChildren()) {
            for (AspectModelPart child : children) {
                child.renderInternal(vcp, currentRenderLayers, matrixStack);
            }
        }

        //Remove the matrix we pushed earlier
        if (hasVertexData() || hasChildren()) {
            matrixStack.pop();
        }
    }




}
