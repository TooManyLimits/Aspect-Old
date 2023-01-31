package io.github.moonlightmaya;

import com.google.common.collect.ImmutableList;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.CreeperEntityRenderer;
import net.minecraft.client.render.entity.model.CreeperEntityModel;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4d;
import org.joml.Vector3d;

import java.util.List;

/**
 * An element of a hierarchical tree structure, analogous to the structure in Blockbench as well as the file system.
 */
public class AspectModelPart {
    private String name;
    public List<AspectModelPart> children; //null if no children //temporarily public for testing!

    private Matrix4d partMatrix;
    private Vector3d partPos;
    private Vector3d partRot;
    private Vector3d partScale;
    public float[] vertexData; //null if no vertices in this part //Temporarily public for testing!

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
     * transformToViewSpace should generally be true when using compatibility mode, and false otherwise.
     */
    public void render(VertexConsumerProvider vcp, boolean transformToViewSpace) {
        List<RenderLayer> defaultLayers = DEFAULT_LAYERS;
        renderInternal(vcp, defaultLayers);
    }

    /**
     * Recursively renders this part and its children to the VCP using compat mode
     * The part customization stack from Figura Rewrite II is removed, in favor of using
     * the call stack with parameters instead. (Just like prewrite did~ lol)
     */
    private void renderInternal(
            VertexConsumerProvider vcp, //The VCP used for this rendering call, where we will fetch buffers from using the render layers.
            List<RenderLayer> currentRenderLayers //The current set of render layers for the part. Inherited from the parent if this.renderLayers is null.

    ) {
        //If this model part's layers are not null, then set the current ones to our overrides. Otherwise, keep the parent's render layers.
        if (this.renderLayers != null)
            currentRenderLayers = this.renderLayers;

        if (hasVertexData()) {
            for (RenderLayer layer : currentRenderLayers) {
                //Obtain a vertex buffer from the VCP, then put all our vertices into it.
                VertexConsumer buffer = vcp.getBuffer(layer);
                for (int i = 0; i < vertexData.length; i += 8) {
                    buffer.vertex(
                            vertexData[i], vertexData[i+1], vertexData[i+2], //Position
                            1f, 1f, 1f, 1f, //Color
                            vertexData[i+3], vertexData[i+4], //Texture
                            OverlayTexture.DEFAULT_UV, //"Overlay"
                            LightmapTextureManager.MAX_LIGHT_COORDINATE, //Light
                            vertexData[i+5], vertexData[i+6], vertexData[i+7] //Normal
                    );
                }
            }
        }

        //If there are children , render them all too
        if (hasChildren()) {
            for (AspectModelPart child : children) {
                child.renderInternal(vcp, currentRenderLayers);
            }
        }
    }




}
