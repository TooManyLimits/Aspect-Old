package io.github.moonlightmaya.model;

import com.google.common.collect.ImmutableList;
import io.github.moonlightmaya.Aspect;
import io.github.moonlightmaya.data.BaseStructures;
import io.github.moonlightmaya.texture.AspectTexture;
import io.github.moonlightmaya.util.AspectMatrixStack;
import net.minecraft.client.render.*;
import net.minecraft.util.Identifier;
import org.joml.*;

import java.util.ArrayList;
import java.util.List;

/**
 * An element of a hierarchical tree structure, analogous to the structure in Blockbench as well as the file system.
 */
public class AspectModelPart {
    private final String name;

    //Unsure whether these following values should use float or double precision.
    //I'll leave them as float for now because double precision shouldn't be required for these parts.
    //Also, this data will need to be uploaded to the GPU.
    //The only case where double precision is necessary is for world model parts.
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

    //Cube data is always position, texture, normal.
    //Mesh data is position, texture, normal, with (optionally) skinning information.
    private static final float[] tempCubeData = new float[(3+2+3+1) * 24]; //max 24 verts per cube
    public float[] vertexData; //null if no vertices in this part. in the case of non-float values like short, we cast the float down.

    public final Aspect owningAspect; //The aspect that this model part is inside

    public AspectModelPart(BaseStructures.ModelPartStructure baseStructure, Aspect owningAspect) {
        this.owningAspect = owningAspect;
        name = baseStructure.name();
        setPos(baseStructure.pos());
        setRot(baseStructure.rot());
        setPivot(baseStructure.pivot());
        if (baseStructure.children() != null) {
            children = new ArrayList<>(baseStructure.children().size());
            for (BaseStructures.ModelPartStructure child : baseStructure.children())
                children.add(new AspectModelPart(child, owningAspect)); //all children are owned by the same aspect
        }
        if (baseStructure.cubeData() != null)
            genCubeRenderData(baseStructure.cubeData());
    }

    public void setPos(Vector3f vec) {
        setPos(vec.x, vec.y, vec.z);
    }

    public void setPos(float x, float y, float z) {
        partPos.set(x, y, z);
        needsMatrixRecalculation = true;
    }

    public void setPivot(Vector3f vec) {
        setPivot(vec.x, vec.y, vec.z);
    }

    public void setPivot(float x, float y, float z) {
        partPivot.set(x, y, z);
        needsMatrixRecalculation = true;
    }

    public void setScale(Vector3f vec) {
        setScale(vec.x, vec.y, vec.z);
    }

    public void setScale(float s) {
        setScale(s, s, s);
    }

    public void setScale(float x, float y, float z) {
        partScale.set(x, y, z);
        needsMatrixRecalculation = true;
    }

    public void setRot(Vector3f vec) {
        setRot(vec.x, vec.y, vec.z);
    }

    public void setRot(float x, float y, float z) {
        partRot.identity().rotationXYZ(x, y, z);
        needsMatrixRecalculation = true;
    }

    public void setRot(Quaternionf rot) {
        partRot.set(rot);
        needsMatrixRecalculation = true;
    }

    /**
     * Generates render data from cube data
     * Including the vertices and the render layers
     */
    private void genCubeRenderData(BaseStructures.CubeData cube) {
        Vector3f f = cube.from();
        Vector3f t = cube.to();
        BaseStructures.CubeFaces faces = cube.faces();
        int idx = 0;
        int w = 0;
        for (int i = 0; i < 6; i++) {
            if ((faces.presentFaces() & (1 << i)) == 0) continue; //face is deleted
            BaseStructures.CubeFace face = faces.faces().get(w++);
            float u1 = face.uvs().x();
            float v1 = face.uvs().y();
            float u2 = face.uvs().z();
            float v2 = face.uvs().y();
            float u3 = face.uvs().z();
            float v3 = face.uvs().w();
            float u4 = face.uvs().x();
            float v4 = face.uvs().w();
            int r = face.rot();
            while (r > 0) { //rotate texture
                float temp = u1;
                u1 = u2; u2 = u3; u3 = u4; u4 = temp;
                temp = v1;
                v1 = v2; v2 = v3; v3 = v4; v4 = temp;
                r--;
            }
            switch (i) {
                case 0 -> { //north
                    idx = emitCubeVert(idx, t.x, t.y, f.z, u1, v1, 0f, 0f, -1f);
                    idx = emitCubeVert(idx, f.x, t.y, f.z, u2, v2,0f, 0f, -1f);
                    idx = emitCubeVert(idx, f.x, f.y, f.z, u3, v3, 0f, 0f, -1f);
                    idx = emitCubeVert(idx, t.x, f.y, f.z, u4, v4, 0f, 0f, -1f);
                }
                case 1 -> { //east
                    idx = emitCubeVert(idx, t.x, t.y, t.z, u1, v1, 1f, 0f, 0f);
                    idx = emitCubeVert(idx, t.x, t.y, f.z, u2, v2,1f, 0f, 0f);
                    idx = emitCubeVert(idx, t.x, f.y, f.z, u3, v3, 1f, 0f, 0f);
                    idx = emitCubeVert(idx, t.x, f.y, t.z, u4, v4, 1f, 0f, 0f);
                }
                case 2 -> { //south
                    idx = emitCubeVert(idx, f.x, t.y, t.z, u1, v1, 0f, 0f, 1f);
                    idx = emitCubeVert(idx, t.x, t.y, t.z, u2, v2, 0f, 0f, 1f);
                    idx = emitCubeVert(idx, t.x, f.y, t.z, u3, v3, 0f, 0f, 1f);
                    idx = emitCubeVert(idx, f.x, f.y, t.z, u4, v4, 0f, 0f, 1f);
                }
                case 3 -> { //west
                    idx = emitCubeVert(idx, f.x, t.y, f.z, u1, v1, -1f, 0f, 0f);
                    idx = emitCubeVert(idx, f.x, t.y, t.z, u2, v2, -1f, 0f, 0f);
                    idx = emitCubeVert(idx, f.x, f.y, t.z, u3, v3, -1f, 0f, 0f);
                    idx = emitCubeVert(idx, f.x, f.y, f.z, u4, v4, -1f, 0f, 0f);
                }
                case 4 -> { //up
                    idx = emitCubeVert(idx, f.x, t.y, f.z, u1, v1, 0f, 1f, 0f);
                    idx = emitCubeVert(idx, t.x, t.y, f.z, u2, v2, 0f, 1f, 0f);
                    idx = emitCubeVert(idx, t.x, t.y, t.z, u3, v3, 0f, 1f, 0f);
                    idx = emitCubeVert(idx, f.x, t.y, t.z, u4, v4, 0f, 1f, 0f);
                }
                case 5 -> { //down
                    idx = emitCubeVert(idx, f.x, f.y, t.z, u1, v1, 0f, -1f, 0f);
                    idx = emitCubeVert(idx, t.x, f.y, t.z, u2, v2, 0f, -1f, 0f);
                    idx = emitCubeVert(idx, t.x, f.y, f.z, u3, v3, 0f, -1f, 0f);
                    idx = emitCubeVert(idx, f.x, f.y, f.z, u4, v4, 0f, -1f, 0f);
                }
            }
        }
        vertexData = new float[idx];
        System.arraycopy(tempCubeData, 0, vertexData, 0, idx);

        //Set up render layer:
        renderLayers = new ArrayList<>();
        AspectTexture tex = owningAspect.textures.get(faces.tex()); //grab the texture
        renderLayers.add(RenderLayer.getEntityCutoutNoCull(tex.getIdentifier())); //make or get render layer for that texture
    }
    //Returns the new ptr
    private int emitCubeVert(int ptr, float x, float y, float z, float u, float v, float nx, float ny, float nz) {
        tempCubeData[ptr] = x/16;
        tempCubeData[ptr+1] = y/16;
        tempCubeData[ptr+2] = z/16;
        tempCubeData[ptr+3] = u;
        tempCubeData[ptr+4] = v;
        tempCubeData[ptr+5] = nx;
        tempCubeData[ptr+6] = ny;
        tempCubeData[ptr+7] = nz;
        return ptr+8;
    }

    /**
     * Contains the render layers we wish to draw to
     * These may be vanilla render layers,
     * or layers for custom core shaders we add,
     * or even custom layers provided by an equipped Aspect.
     *
     * If this is null, then copy the render layers of the parent.
     */
    private List<RenderLayer> renderLayers;

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
                    .translate(-partPivot.x, -partPivot.y, -partPivot.z);
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
     * Depending on whether we're in optimized or compatible mode, the provided matrixStack may also be
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
                    Vector4d pos = new Vector4d(vertexData[i], vertexData[i+1], vertexData[i+2], 1f);
                    matrixStack.peekPosition().transform(pos);
                    Vector3f normal = new Vector3f(vertexData[i+5], vertexData[i+6], vertexData[i+7]);
                    matrixStack.peekNormal().transform(normal);
                    buffer.vertex(
                            (float) pos.x, (float) pos.y, (float) pos.z, //Position
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


    public enum ModelPartType {
        GROUP,
        CUBE,
        MESH,
        NULL
    }

}
