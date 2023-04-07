package io.github.moonlightmaya.model;

import com.google.common.collect.ImmutableList;
import io.github.moonlightmaya.Aspect;
import io.github.moonlightmaya.data.BaseStructures;
import io.github.moonlightmaya.texture.AspectTexture;
import io.github.moonlightmaya.util.AspectMatrixStack;
import io.github.moonlightmaya.vanilla.VanillaPart;
import net.minecraft.client.render.*;
import net.minecraft.util.Identifier;
import org.joml.*;
import petpet.external.PetPetWhitelist;
import petpet.lang.run.PetPetException;
import petpet.types.PetPetList;

import java.lang.Math;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An element of a hierarchical tree structure, analogous to the structure in Blockbench as well as the file system.
 */
@PetPetWhitelist
public class AspectModelPart {
    @PetPetWhitelist
    public final String name;
    @PetPetWhitelist(forceImmutable = true)
    public AspectModelPart parent;

    private final ModelPartType type;

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
    @PetPetWhitelist
    public Boolean visible = null;

    @PetPetWhitelist
    public VanillaPart vanillaParent; //The vanilla part that this will take transforms from

    //Whether this part needs its matrix recalculated. After calling rot(), pos(), etc. this will be set to true.
    //If it's true at rendering time, then the this.partMatrix field will be updated, and this will be set to false.
    public boolean needsMatrixRecalculation = true;

    //Cube data is always position, texture, normal.
    //Mesh data is position, texture, normal, with (optionally) skinning information.
    //max 24 verts per cube, made into a thread local because multiple threads can build vertex data at once
    private static final ThreadLocal<float[]> sharedTempCubeData = ThreadLocal.withInitial(() -> new float[(3+2+3+1) * 24]);
    private float[] tempCubeData;
    public float[] vertexData; //null if no vertices in this part. in the case of non-float values like short, we cast the float down.

    public final Aspect owningAspect; //The aspect that this model part is inside

    public AspectModelPart(BaseStructures.ModelPartStructure baseStructure, Aspect owningAspect, AspectModelPart parent) {
        this.owningAspect = owningAspect;
        name = baseStructure.name();
        type = baseStructure.type();
        setPos(baseStructure.pos());
        setRot(baseStructure.rot());
        this.parent = parent;
        if (parent == null) {
            visible = null; //only possible for mod-generated parts, not blockbench ones
        } else {
            boolean parentVis = parent.recursiveParentVisibility();
            if (parentVis == baseStructure.visible())
                visible = null;
            else
                visible = baseStructure.visible();
        }

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

        //Perhaps not necessary, and can be done through a petpet script run at startup!
//        if (type == ModelPartType.GROUP) {
//            for (Map.Entry<Object, VanillaPart> entry : owningAspect.vanillaRenderer.vanillaParts.entrySet()) {
//                if (entry.getKey() instanceof String str) {
//                    if (name.substring(0, Math.min(str.length(), name.length())).equalsIgnoreCase(str)) {
//                        vanillaParent = entry.getValue();
//                        break;
//                    }
//                }
//            }
//        }

    }

    /**
     * This is a bit strange but bear with me
     * The default value for parent in the *Base Structure* is true.
     * Even though the constructor sets it to null, we should compare against true.
     * This method is only used in the constructor.
     */
    private Boolean recursiveParentVisibility() {
        if (parent == null) return true;
        if (visible != null) return visible;
        return parent.recursiveParentVisibility();
    }

    public void setPos(Vector3f vec) {
        setPos(vec.x, vec.y, vec.z);
    }
    public void setPos(Vector3d vec) {
        setPos((float) vec.x, (float) vec.y, (float) vec.z);
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
        if (needsMatrixRecalculation || vanillaParent != null) {
            //Scale down the pivot value, it's in "block" units
            positionMatrix.translation(partPivot.mul(1f/16));

            if (vanillaParent != null) {
                positionMatrix.mul(vanillaParent.inverseDefaultTransform);
                positionMatrix.mul(vanillaParent.savedTransform);
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
        renderInternal(vcp, defaultLayers, matrixStack, true, light);
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
            boolean visible, //Current visibility of the part. Just like render layers, inherited from parent if this.visible is null.
            int light //The light level of the entity wearing the aspect. Inherited from the parent always (for the time being)
    ) {
        //If this model part's layers are not null, then set the current ones to our overrides. Otherwise, keep the parent's render layers.
        if (this.renderLayers != null)
            currentRenderLayers = this.renderLayers;
        //Likewise for visibility.
        if (this.visible != null)
            visible = this.visible;

        //Push and transform the stack, if necessary
        if (hasVertexData() || hasChildren()) {
            //Recalculate the matrices for this part if necessary, then apply them to the stack.
            recalculateMatrixIfNecessary();
            matrixStack.push();
            matrixStack.multiply(positionMatrix, normalMatrix);
        }

        //Render the part only if it's visible and has vertex data
        if (hasVertexData() && visible) {
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
                child.renderInternal(vcp, currentRenderLayers, matrixStack, visible, light);
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
    public AspectModelPart rot_3(double x, double y, double z) {
        setRot((float) x, (float) y, (float) z);
        return this;
    }
    @PetPetWhitelist
    public AspectModelPart rot_1(Object r) {
        if (r instanceof Quaterniond quat) {
            setRot(quat);
            return this;
        } else if (r instanceof Vector3d v) {
            return rot_3(v.x, v.y, v.z);
        }
        //Wish I could get the PetPetClass's name, but this will have to do
        throw new PetPetException("Attempt to call rot() with object that is not vec3 or quat. type is " + r.getClass().getSimpleName());
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
    public AspectModelPart piv_3(double x, double y, double z) {
        setPivot((float) x, (float) y, (float) z);
        return this;
    }
    @PetPetWhitelist
    public AspectModelPart piv_1(Vector3d v) {
        return piv_3(v.x, v.y, v.z);
    }

    @PetPetWhitelist
    public String bbType() {
        return type.name();
    }

    @PetPetWhitelist
    public PetPetList<AspectModelPart> getChildren() {
        PetPetList<AspectModelPart> childrenCopy = new PetPetList<>();
        childrenCopy.addAll(children);
        return childrenCopy;
    }

    @PetPetWhitelist
    public AspectModelPart vanillaParent(VanillaPart vanillaPart) {
        vanillaParent = vanillaPart;
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

    @Override
    public String toString() {
        return "ModelPart(" + name + ")";
    }
}
