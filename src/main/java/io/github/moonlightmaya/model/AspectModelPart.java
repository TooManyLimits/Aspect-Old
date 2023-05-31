package io.github.moonlightmaya.model;

import com.google.common.collect.ImmutableList;
import io.github.moonlightmaya.Aspect;
import io.github.moonlightmaya.manage.data.BaseStructures;
import io.github.moonlightmaya.model.animation.Animator;
import io.github.moonlightmaya.model.rendertasks.BlockTask;
import io.github.moonlightmaya.model.rendertasks.ItemTask;
import io.github.moonlightmaya.model.rendertasks.RenderTask;
import io.github.moonlightmaya.model.rendertasks.TextTask;
import io.github.moonlightmaya.util.AspectMatrixStack;
import io.github.moonlightmaya.util.RenderUtils;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.*;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.joml.*;
import petpet.external.PetPetWhitelist;
import petpet.lang.run.PetPetCallable;
import petpet.lang.run.PetPetException;
import petpet.types.PetPetList;

import java.lang.Math;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/**
 * An element of a hierarchical tree structure, analogous to the structure in Blockbench as well as the file system.
 */
@PetPetWhitelist
public class AspectModelPart extends Transformable {

    public final String name;
    public AspectModelPart parent;

    private final ModelPartType type;

    //Unsure whether these following values should use float or double precision.
    //I'll leave them as float for now because double precision shouldn't be required for these parts.
    //Also, this data will need to be uploaded to the GPU.
    //The only case where double precision is necessary is for world model parts.
    public PetPetList<AspectModelPart> children; //null if no children
    public final Matrix3f uvMatrix = new Matrix3f();
    private PetPetCallable preRender, postRender;
    private final PetPetList<RenderTask> renderTasks = new PetPetList<>();


    //Whether this part needs its matrix recalculated. After calling rot(), pos(), etc. this will be set to true.
    //If it's true at rendering time, then the this.partMatrix field will be updated, and this will be set to false.
    public boolean needsMatrixRecalculation = true;

    private final Matrix4d partToWorldMatrix = new Matrix4d();

    //Cube data is always position, texture, normal.
    //Mesh data is position, texture, normal, with (optionally) skinning information.
    //max 24 verts per cube, made into a thread local because multiple threads can build vertex data at once
    private static final ThreadLocal<float[]> sharedTempCubeData = ThreadLocal.withInitial(() -> new float[(3+2+3+1) * 24]);
    private final float[] tempCubeData;
    public float[] vertexData; //null if no vertices in this part. in the case of non-float values like short, we cast the float down.

    public final Aspect owningAspect; //The aspect that this model part is inside

    public AspectModelPart(BaseStructures.ModelPartStructure baseStructure, Aspect owningAspect, AspectModelPart parent) {
        this.owningAspect = owningAspect;
        name = baseStructure.name();
        type = baseStructure.type();
        setRot(baseStructure.rot());
        setPivot(baseStructure.pivot());

        //Process animators
        for (BaseStructures.AnimatorStructure animatorStructure : baseStructure.animators()) {
            this.transformers.add(new Animator(animatorStructure, owningAspect));
        }

        this.parent = parent;
        visible = baseStructure.visible();

        if (baseStructure.children() != null) {
            children = new PetPetList<>(baseStructure.children().size());
            for (BaseStructures.ModelPartStructure child : baseStructure.children()) {
                //all children are owned by the same aspect
                children.add(new AspectModelPart(child, owningAspect, this));
            }
            //If all children have the same render layers, then merge them and set this to have it instead
            if (children.size() > 0) {
                boolean allSame = true;
                PetPetList<?> child0Layers = children.get(0).renderLayers;
                for (int i = 1; i < children.size(); i++)
                    if (!arePetPetListsEqual(child0Layers, children.get(i).renderLayers)) {
                        allSame = false;
                        break;
                    }
                //If all of the children have the same layers, then
                if (allSame && child0Layers != null) {
                    this.renderLayers = (PetPetList<RenderLayer>) child0Layers.clone();
                    for (AspectModelPart child : this.children)
                        child.renderLayers = null;
                }
            }
        }

        //Cache the thread local array to avoid a map lookup on each cube vertex
        //maybe unnecessary, but might as well? i guess? i mean i already programmed it so why not
        tempCubeData = sharedTempCubeData.get();
        if (baseStructure.cubeData() != null)
            genCubeRenderData(baseStructure.cubeData());

        //If no cube data, generate mesh data if it exists
        else if (baseStructure.meshData() != null)
            genMeshRenderData(baseStructure.meshData());
    }

    private AspectModelPart(AspectModelPart base, String newPartName, boolean deepCopyChildList, boolean deepCopyVertices) {
        this.owningAspect = base.owningAspect;
        name = newPartName;
        type = base.type;
        tempCubeData = base.tempCubeData;
        parent = base.parent;
        renderLayers = base.renderLayers;

        if (deepCopyChildList) {
            children = new PetPetList<>(base.children.size());
            children.addAll(base.children);
        } else {
            children = base.children;
        }

        if (deepCopyVertices) {
            if (base.vertexData == null) {
                vertexData = null;
            } else {
                vertexData = new float[base.vertexData.length];
                System.arraycopy(base.vertexData, 0, vertexData, 0, vertexData.length);
            }
        } else {
            vertexData = base.vertexData;
        }

        //Clone the transformable properties over
        cloneFrom(base);
    }

    public void setUV(float x, float y) {
        uvMatrix.identity();
        uvMatrix.set(2, 0, x);
        uvMatrix.set(2, 1, y);
    }

    public void setUVMatrix(Matrix3d matrix) {
        uvMatrix.set(
                (float) matrix.m00, (float) matrix.m01, (float) matrix.m02,
                (float) matrix.m10, (float) matrix.m11, (float) matrix.m12,
                (float) matrix.m20, (float) matrix.m21, (float) matrix.m22
        );
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

        //Set up default render layer:
        renderLayers = new PetPetList<>();
        AspectTexture tex = owningAspect.textures.get(faces.tex()); //grab the texture
        //Default layer is cutout-no-cull with the texture
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

    private void genMeshRenderData(BaseStructures.MeshData meshData) {
        Vector3f temp1 = new Vector3f();
        Vector3f temp2 = new Vector3f();
        int curUV = 0;
        FloatArrayList floats = new FloatArrayList();
        for (Vector4i face : meshData.faces()) {
            boolean isQuad = face.w != -1;

            Vector3f a = meshData.vertexPositions().get(face.x);
            Vector2f aUV = meshData.uvs().get(curUV++);
            Vector3f b = meshData.vertexPositions().get(face.y);
            Vector2f bUV = meshData.uvs().get(curUV++);
            Vector3f c = meshData.vertexPositions().get(face.z);
            Vector2f cUV = meshData.uvs().get(curUV++);

            temp2.set(c).sub(a);
            Vector3f normal = temp1.set(b).sub(a).cross(temp2).normalize();

            if (isQuad) { //If quad, add the 4 vertices
                Vector3f d = meshData.vertexPositions().get(face.w);
                Vector2f dUV = meshData.uvs().get(curUV++);
                addVertex(floats, a, aUV, normal);
                addVertex(floats, b, bUV, normal);
                addVertex(floats, c, cUV, normal);
                addVertex(floats, d, dUV, normal);
            } else { //Triangle:
                //Duplicate the 4th vertex, to make it a "quad" for minecraft rendering
                addVertex(floats, a, aUV, normal);
                addVertex(floats, b, bUV, normal);
                addVertex(floats, c, cUV, normal);
                addVertex(floats, c, cUV, normal);
            }
        }

        //Make vertices float[]
        this.vertexData = floats.toFloatArray();

        //Set up default render layer:
        renderLayers = new PetPetList<>();
        AspectTexture tex = owningAspect.textures.get(meshData.tex()); //grab the texture
        renderLayers.add(RenderLayer.getEntityCutoutNoCull(tex.getIdentifier())); //make or get render layer for that texture
    }

    private void addVertex(FloatArrayList floats, Vector3f pos, Vector2f uv, Vector3f normal) {
        floats.add((pos.x + this.partPivot.x) / 16);
        floats.add((pos.y + this.partPivot.y) / 16);
        floats.add((pos.z + this.partPivot.z) / 16);
        floats.add(uv.x);
        floats.add(uv.y);
        floats.add(normal.x);
        floats.add(normal.y);
        floats.add(normal.z);
    }

    /**
     * Contains the render layers we wish to draw to
     * These may be vanilla render layers,
     * or layers for custom core shaders we add,
     * or even custom layers provided by an equipped Aspect.
     *
     * If this is null, then copy the render layers of the parent.
     */
    private PetPetList<RenderLayer> renderLayers;

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
     * Depending on whether we're in optimized or compatible mode, the provided matrixStack may also be
     * different.
     *
     * Visibility defaults to true for the moment, at the root. May change in the future.
     *
     * The light level is the default light level to render the model part "root" at.
     * In the future, this may be overridden by specific model part customizations.
     */
    public void render(VertexConsumerProvider vcp, AspectMatrixStack matrixStack, int light, int overlay) {
        List<RenderLayer> defaultLayers = DEFAULT_LAYERS;
        renderInternal(vcp, defaultLayers, matrixStack, light, overlay);
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
            AspectMatrixStack matrixStack, //The current matrix stack.
            int light, //The light level of the entity wearing the aspect. Inherited from the parent always (for the time being)
            int overlay
    ) {
        //Push the matrix stack
        matrixStack.push();

        //Run the callback
        if (preRender != null) {
            try {
                if (owningAspect.scriptHandler != null && !owningAspect.scriptHandler.isErrored())
                    preRender.call(this, matrixStack.peekPosition(), matrixStack.peekNormal());
            } catch (Exception e) {
                owningAspect.scriptHandler.error(e);
            }
        }

        //If this model part's layers are not null, then set the current ones to our overrides. Otherwise, keep the parent's render layers.
        if (this.renderLayers != null)
            currentRenderLayers = this.renderLayers;

        //If we're not visible, then return.
        if (!this.visible) {
            //Call the post render if it exists
            if (postRender != null) {
                try {
                    if (owningAspect.scriptHandler != null && !owningAspect.scriptHandler.isErrored())
                        postRender.call(this, matrixStack.peekPosition(), matrixStack.peekNormal());
                } catch (Exception e) {
                    owningAspect.scriptHandler.error(e);
                }
            }
            matrixStack.pop();
            return;
        }

        //Push and transform the stack, if necessary
        if (hasVertexData() || hasChildren() || renderTasks.size() > 0) {
            //Recalculate the matrices for this part if necessary, then apply them to the stack.
            recalculateMatrixIfNeeded();
            matrixStack.multiply(positionMatrix, normalMatrix);
            matrixStack.multiplyUV(uvMatrix);
        }

        //If we're in the proper render mode, then save the part to world matrices
        if (owningAspect.renderContext.equals(Aspect.RenderContexts.WORLD)) {
            this.partToWorldMatrix.set(matrixStack.peekPosition());
            this.partToWorldMatrix.mulLocal(RenderUtils.VIEW_TO_WORLD_MATRIX);
            this.partToWorldMatrix.scale(1.0/16);
            this.partToWorldMatrix.translate(this.partPivot);
        }

        for (RenderTask task : renderTasks)
            task.render(matrixStack, vcp, light, overlay);

        //Render the part only if it's visible and has vertex data
        if (hasVertexData()) {
            Vector4d pos = new Vector4d();
            Vector3f normal = new Vector3f();
            Vector3f uv = new Vector3f();
            for (RenderLayer layer : currentRenderLayers) {
                //Obtain a vertex buffer from the VCP, then put all our vertices into it.
                VertexConsumer buffer = vcp.getBuffer(layer);
                for (int i = 0; i < vertexData.length; i += 8) {
                    pos.set(vertexData[i], vertexData[i+1], vertexData[i+2], 1f);
                    matrixStack.peekPosition().transform(pos);
                    normal.set(vertexData[i+5], vertexData[i+6], vertexData[i+7]);
                    matrixStack.peekNormal().transform(normal);
                    uv.set(vertexData[i+3], vertexData[i+4], 1f);
                    matrixStack.peekUV().transform(uv);

                    buffer.vertex(
                            (float) pos.x, (float) pos.y, (float) pos.z, //Position
                            1f, 1f, 1f, 1f, //Color
                            uv.x, uv.y, //Texture
                            overlay, //"Overlay"
                            light, //Light
                            normal.x, normal.y, normal.z //Normal
                    );
                }
            }
        }

        //If there are children, render them all too
        if (hasChildren()) {
            for (AspectModelPart child : children) {
                child.renderInternal(vcp, currentRenderLayers, matrixStack, light, overlay);
            }
        }

        if (postRender != null) {
            try {
                if (owningAspect.scriptHandler != null && !owningAspect.scriptHandler.isErrored())
                    postRender.call(this, matrixStack.peekPosition(), matrixStack.peekNormal());
            } catch (Exception e) {
                owningAspect.scriptHandler.error(e);
            }
        }


        //Remove the matrix we pushed earlier
        if (hasVertexData() || hasChildren()) {
            matrixStack.pop();
        }
    }

    //Small helper, can't use .equals() since PetPetList overrides it to
    //literal equality
    private static boolean arePetPetListsEqual(PetPetList<?> a, PetPetList<?> b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++)
            if (!Objects.equals(a.get(i), b.get(i)))
                return false;
        return true;
    }

    public enum ModelPartType {
        GROUP,
        CUBE,
        MESH,
        NULL_OBJECT,
        LOCATOR,
    }

    //Meshes have different rotation order from
    //other model parts for no reason, these
    //overrides account for that
    @Override
    public void setRot(float x, float y, float z) {
        if (type != ModelPartType.MESH)
            super.setRot(x, y, z);
        else {
            float s = (float) (Math.PI / 180);
            partRot.identity().rotationXYZ(x * s, y * s, z * s);
            needsMatrixRecalculation = true;
        }
    }
    @PetPetWhitelist
    @Override
    public Vector3d rot_0() {
        if (type != ModelPartType.MESH)
            return super.rot_0();
        else {
            return new Vector3d(this.partRot.getEulerAnglesZYX(new Vector3f()));
        }
    }


    //----------PETPET FUNCTIONS----------//
    //----------PETPET FUNCTIONS----------//
    //----------PETPET FUNCTIONS----------//
    //----------PETPET FUNCTIONS----------//
    //----------PETPET FUNCTIONS----------//


    /**
     * Other properties:
     * - uv
     * - visibility
     * - todo: color
     * - vanilla parent
     * - pre/post render functions
     */

    @PetPetWhitelist
    public AspectModelPart uv_2(double x, double y) {
        setUV((float) x, (float) y);
        return this;
    }
    @PetPetWhitelist
    public AspectModelPart uv_1(Vector2d vec) {
        return uv_2(vec.x, vec.y);
    }
    @PetPetWhitelist
    public Vector2d uv_0() {
        return new Vector2d(uvMatrix.m00, uvMatrix.m11);
    }

    @PetPetWhitelist
    public AspectModelPart uvMatrix_1(Matrix3d mat) {
        this.setUVMatrix(mat);
        return this;
    }
    @PetPetWhitelist
    public Matrix3d uvMatrix_0() {
        return new Matrix3d(uvMatrix);
    }

    @PetPetWhitelist
    public PetPetCallable preRender_0() {
        return preRender;
    }

    @PetPetWhitelist
    public AspectModelPart preRender_1(PetPetCallable callable) {
        if (callable == null) {
            this.preRender = null;
            return this;
        }
        if (callable.paramCount() != 3)
            throw new PetPetException("preRender callback expects 3 args: part, posMatrix, normalMatrix");
        preRender = callable;
        return this;
    }

    @PetPetWhitelist
    public PetPetCallable postRender_0() {
        return postRender;
    }

    @PetPetWhitelist
    public AspectModelPart postRender_1(PetPetCallable callable) {
        if (callable == null) {
            this.postRender = null;
            return this;
        }
        if (callable.paramCount() != 3)
            throw new PetPetException("postRender callback expects 3 args: part, posMatrix, normalMatrix");
        postRender = callable;
        return this;
    }

    /**
     * Render tasks
     */
    @PetPetWhitelist
    public PetPetList<RenderTask> renderTasks() {
        return renderTasks;
    }
    @PetPetWhitelist
    public AspectModelPart removeTask(RenderTask task) {
        renderTasks.remove(task);
        return this;
    }
    @PetPetWhitelist
    public BlockTask newBlock(BlockState state) {
        BlockTask task = new BlockTask(state);
        renderTasks.add(task);
        return task;
    }
    @PetPetWhitelist
    public ItemTask newItem(ItemStack stack) {
        ItemTask task = new ItemTask(stack);
        renderTasks.add(task);
        return task;
    }
    @PetPetWhitelist
    public TextTask newText(String text) {
        TextTask task = new TextTask(text);
        renderTasks.add(task);
        return task;
    }


    /**
     * Informational getters
     */

    @PetPetWhitelist
    public String bbType() {
        return type.name();
    }

    @PetPetWhitelist
    public PetPetList<AspectModelPart> children() {
        return children;
    }

    @PetPetWhitelist
    public String name() {
        return name;
    }

    @PetPetWhitelist
    public AspectModelPart parent() {
        return parent;
    }

    /**
     * Unlike figura, returns the actual part to world matrix,
     * not a copy. If you want a copy then copy it.
     */
    @PetPetWhitelist
    public Matrix4d partToWorldMatrix() {
        return this.partToWorldMatrix;
    }

    /**
     * Other manipulation operations
     */

    @PetPetWhitelist
    public AspectModelPart clearChildCache() {
        if (cachedPartMap != null)
            cachedPartMap.clear();
        return this;
    }

    @PetPetWhitelist
    public AspectModelPart copy_1(String name) {
        return new AspectModelPart(this, name, false, false);
    }

    @PetPetWhitelist
    public AspectModelPart copy_3(String name, boolean deepCopyChildList, boolean deepCopyVertices) {
        return new AspectModelPart(this, name, deepCopyChildList, deepCopyVertices);
    }

    /**
     * Render layers
     */
    @PetPetWhitelist
    public AspectModelPart renderLayers_1(PetPetList<RenderLayer> layers) {
        this.renderLayers = layers;
        return this;
    }

    @PetPetWhitelist
    public PetPetList<RenderLayer> renderLayers_0() {
        return this.renderLayers;
    }


    /**
     * Metamethods/overloads
     */

    private HashMap<String, AspectModelPart> cachedPartMap;
    @PetPetWhitelist
    public AspectModelPart __get_str(String arg) {
        if (children == null) return null;
        if (cachedPartMap == null) cachedPartMap = new HashMap<>();
        return cachedPartMap.computeIfAbsent(arg, s -> {
            for (AspectModelPart child : children) {
                if (child.name.equals(s))
                    return child;
            }
            return null;
        });
    }

    @PetPetWhitelist
    public AspectModelPart __get_num(int arg) {
        if (children == null) return null;
        if (arg < 0 || arg >= children.size()) return null;
        return children.get(arg);
    }

    @Override
    public String toString() {
        return "ModelPart(" + name + ")";
    }
}
