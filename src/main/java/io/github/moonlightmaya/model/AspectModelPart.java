package io.github.moonlightmaya.model;

import com.google.common.collect.ImmutableList;
import io.github.moonlightmaya.Aspect;
import io.github.moonlightmaya.data.BaseStructures;
import io.github.moonlightmaya.script.AspectScriptHandler;
import io.github.moonlightmaya.texture.AspectTexture;
import io.github.moonlightmaya.util.AspectMatrixStack;
import io.github.moonlightmaya.vanilla.VanillaPart;
import net.minecraft.client.render.*;
import net.minecraft.util.Identifier;
import org.joml.*;
import petpet.external.PetPetWhitelist;
import petpet.lang.run.PetPetCallable;
import petpet.lang.run.PetPetException;
import petpet.types.PetPetList;

import java.lang.Math;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * An element of a hierarchical tree structure, analogous to the structure in Blockbench as well as the file system.
 */
@PetPetWhitelist
public class AspectModelPart {

    public final String name;
    public AspectModelPart parent;

    private final ModelPartType type;

    //Unsure whether these following values should use float or double precision.
    //I'll leave them as float for now because double precision shouldn't be required for these parts.
    //Also, this data will need to be uploaded to the GPU.
    //The only case where double precision is necessary is for world model parts.
    public PetPetList<AspectModelPart> children; //null if no children
    public final Matrix4f positionMatrix = new Matrix4f();
    public final Matrix3f normalMatrix = new Matrix3f();
    public final Matrix3f uvMatrix = new Matrix3f();
    public final Vector3f partPivot = new Vector3f();
    public final Vector3f partPos = new Vector3f();
    public final Quaternionf partRot = new Quaternionf();
    public final Vector3f partScale = new Vector3f(1, 1, 1);
    public boolean visible = true;

    private PetPetCallable preRender, postRender;

    public VanillaPart vanillaParent; //The vanilla part that this will take transforms from

    //Whether this part needs its matrix recalculated. After calling rot(), pos(), etc. this will be set to true.
    //If it's true at rendering time, then the this.partMatrix field will be updated, and this will be set to false.
    public boolean needsMatrixRecalculation = true;

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
        setPos(baseStructure.pos());
        setRot(baseStructure.rot());
        this.parent = parent;
        visible = baseStructure.visible();

        setPivot(baseStructure.pivot());
        if (baseStructure.children() != null) {
            children = new PetPetList<>(baseStructure.children().size());
            for (BaseStructures.ModelPartStructure child : baseStructure.children()) {
                //all children are owned by the same aspect
                children.add(new AspectModelPart(child, owningAspect, this));
            }
        }

        //Cache the thread local array to avoid a map lookup on each cube vertex
        //maybe unnecessary, but might as well? i guess? i mean i already programmed it so why not
        tempCubeData = sharedTempCubeData.get();
        if (baseStructure.cubeData() != null)
            genCubeRenderData(baseStructure.cubeData());

    }

    private AspectModelPart(AspectModelPart base, String newPartName, boolean deepCopyChildList, boolean deepCopyVertices) {
        this.owningAspect = base.owningAspect;
        name = newPartName;
        type = base.type;
        tempCubeData = base.tempCubeData;
        parent = base.parent;
        if (base.renderLayers != null) {
            renderLayers = new PetPetList<>();
            renderLayers.addAll(base.renderLayers);
        }

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

        partPos.set(base.partPos);
        partRot.set(base.partRot);
        partScale.set(base.partScale);
        partPivot.set(base.partPivot);
        positionMatrix.set(base.positionMatrix);
        normalMatrix.set(base.normalMatrix);
        needsMatrixRecalculation = base.needsMatrixRecalculation;

        visible = base.visible;
    }

    public void setPos(Vector3f vec) {
        setPos(vec.x, vec.y, vec.z);
    }
    public void setPos(Vector3d vec) {
        setPos((float) vec.x, (float) vec.y, (float) vec.z);
    }

    public void setPos(float x, float y, float z) {
        partPos.set(x / 16f, y / 16f, z / 16f);
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
        float s = (float) (Math.PI / 180);
        partRot.identity().rotationXYZ(x * s, y * s, z * s);
        needsMatrixRecalculation = true;
    }

    public void setRot(Quaternionf rot) {
        partRot.set(rot);
        needsMatrixRecalculation = true;
    }

    public void setRot(Quaterniond rot) {
        partRot.set(rot);
        needsMatrixRecalculation = true;
    }

    public void setUV(float x, float y) {
        uvMatrix.scaling(x, y, 1);
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

        //Set up render layer:
        renderLayers = new PetPetList<>();
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

    private static final Matrix4f tempMatrixSavedTransform = new Matrix4f();
    private void recalculateMatrix() {
        //Scale down the pivot value, it's in "block" units
        positionMatrix.translation(partPivot.mul(1f/16));

        if (vanillaParent != null) {
            positionMatrix.mul(vanillaParent.inverseDefaultTransform);
            tempMatrixSavedTransform.set(vanillaParent.savedTransform);
            positionMatrix.mul(tempMatrixSavedTransform);
        }

        positionMatrix
                .rotate(partRot)
                .scale(partScale)
                .translate(partPos)
                .translate(-partPivot.x, -partPivot.y, -partPivot.z);

        //Scale the pivot value back up again
        partPivot.mul(16f);
        //Compute the normal matrix as well and store it
        positionMatrix.normal(normalMatrix);
        //Matrices are now calculated, don't need to be recalculated anymore for this part
        needsMatrixRecalculation = false;
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
    public void render(VertexConsumerProvider vcp, AspectMatrixStack matrixStack, int light) {
        List<RenderLayer> defaultLayers = DEFAULT_LAYERS;
        renderInternal(vcp, defaultLayers, matrixStack, light);
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
            int light //The light level of the entity wearing the aspect. Inherited from the parent always (for the time being)
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
        if (hasVertexData() || hasChildren()) {
            //Recalculate the matrices for this part if necessary, then apply them to the stack.
            if (needsMatrixRecalculation || vanillaParent != null)
                recalculateMatrix();

            matrixStack.multiply(positionMatrix, normalMatrix);
        }

        //Render the part only if it's visible and has vertex data
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
                            light, //Light
                            normal.x, normal.y, normal.z //Normal
                    );
                }
            }
        }

        //If there are children, render them all too
        if (hasChildren()) {
            for (AspectModelPart child : children) {
                child.renderInternal(vcp, currentRenderLayers, matrixStack, light);
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


    public enum ModelPartType {
        GROUP,
        CUBE,
        MESH,
        NULL
    }


    //----------PETPET FUNCTIONS----------//
    //----------PETPET FUNCTIONS----------//
    //----------PETPET FUNCTIONS----------//
    //----------PETPET FUNCTIONS----------//
    //----------PETPET FUNCTIONS----------//

    //Setters
    @PetPetWhitelist
    public AspectModelPart pos_3(double x, double y, double z) {
        setPos((float) x, (float) y, (float) z);
        return this;
    }
    @PetPetWhitelist
    public AspectModelPart pos_1(Vector3d v) {
        return pos_3(v.x, v.y, v.z);
    }
    @PetPetWhitelist
    public Vector3d pos_0() {
        return new Vector3d(partPos).mul(16d);
    }

    @PetPetWhitelist
    public AspectModelPart rot_3(double x, double y, double z) {
        setRot((float) x, (float) y, (float) z);
        return this;
    }
    @PetPetWhitelist
    public AspectModelPart rot_1(Vector3d v) {
        return rot_3(v.x, v.y, v.z);
    }

    @PetPetWhitelist
    public Vector3d rot_0() {
        return new Vector3d(partRot.getEulerAnglesXYZ(new Vector3f()));
    }

    //@PetPetWhitelist
    public AspectModelPart quat_1(Quaterniond quat) {
        setRot(quat);
        return this;
    }
    //@PetPetWhitelist
    public Quaterniond quat_0() {
        return new Quaterniond(partRot);
    }

    @PetPetWhitelist
    public AspectModelPart scale_3(double x, double y, double z) {
        setScale((float) x, (float) y, (float) z);
        return this;
    }
    @PetPetWhitelist
    public AspectModelPart scale_1(Vector3d v) {
        return scale_3(v.x, v.y, v.z);
    }
    @PetPetWhitelist
    public Vector3d scale_0() {
        return new Vector3d(partScale);
    }
    @PetPetWhitelist
    public AspectModelPart piv_3(double x, double y, double z) {
        setPivot((float) x, (float) y, (float) z);
        return this;
    }
    @PetPetWhitelist
    public AspectModelPart piv_1(Vector3d v) {
        return piv_3(v.x, v.y, v.z);
    }
    @PetPetWhitelist
    public Vector3d piv_0(Vector3d v) {
        return new Vector3d(partPivot);
    }

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
    public String bbType() {
        return type.name();
    }

    @PetPetWhitelist
    public AspectModelPart visible_1(Boolean b) {
        this.visible = b;
        return this;
    }

    @PetPetWhitelist
    public Boolean visible_0() {
        return visible;
    }

    @PetPetWhitelist
    public AspectModelPart matrix_1(Matrix4d mat) {
        this.positionMatrix.set(mat);
        this.positionMatrix.normal(normalMatrix);
        partPos.zero();
        partRot.identity();
        partPivot.zero();
        partScale.set(1);
        this.needsMatrixRecalculation = false;
        return this;
    }

    @PetPetWhitelist
    public Matrix4d matrix_0() {
        recalculateMatrix(); //recalculate before
        return rawMatrix();
    }

    @PetPetWhitelist
    public Matrix4d rawMatrix() {
        return new Matrix4d(positionMatrix);
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
    public PetPetList<AspectModelPart> getChildren() {
        return children;
    }

    @PetPetWhitelist
    public AspectModelPart clearChildCache() {
        if (cachedPartMap != null)
            cachedPartMap.clear();
        return this;
    }

    @PetPetWhitelist
    public AspectModelPart vanillaParent_1(VanillaPart vanillaPart) {
        vanillaParent = vanillaPart;
        return this;
    }

    @PetPetWhitelist
    public VanillaPart vanillaParent_0() {
        return vanillaParent;
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

    @PetPetWhitelist
    public AspectModelPart copy_1(String name) {
        return new AspectModelPart(this, name, false, false);
    }

    @PetPetWhitelist
    public AspectModelPart copy_3(String name, boolean deepCopyChildList, boolean deepCopyVertices) {
        return new AspectModelPart(this, name, deepCopyChildList, deepCopyVertices);
    }

    @PetPetWhitelist
    public String name() {
        return name;
    }

    @PetPetWhitelist
    public AspectModelPart parent() {
        return parent;
    }

    @Override
    public String toString() {
        return "ModelPart(" + name + ")";
    }
}
