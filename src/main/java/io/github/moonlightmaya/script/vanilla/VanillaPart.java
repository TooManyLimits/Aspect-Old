package io.github.moonlightmaya.script.vanilla;

import io.github.moonlightmaya.mixin.render.vanilla.part.ModelPartAccessor;
import io.github.moonlightmaya.model.Transformable;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelTransform;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import petpet.external.PetPetWhitelist;
import petpet.types.PetPetTable;
import petpet.types.immutable.PetPetTableView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Interface for an aspect to interact with vanilla ModelPart objects
 */
@PetPetWhitelist
public class VanillaPart extends Transformable implements Transformable.Transformer {

    public @Nullable ModelPart referencedPart;
    public final String name;
    public VanillaRenderer renderer;

    //backing map for the children
    //Don't want the actual children map accessible by scripts, because
    //it may mess with the data in a way we don't like
    private final HashMap<String, VanillaPart> childrenBacking = new PetPetTable<>();

    /**
     * When we press F3+T, the entity renderers reload, and the instances
     * of ModelPart change. As such, we need to update our VanillaPart
     * instances to match the new model parts.
     */
    public void update(ModelPart newPart) {
        //Name shouldn't change
        this.referencedPart = newPart;
        for (Map.Entry<String, ModelPart> childEntry : ((ModelPartAccessor) (Object) newPart).getChildren().entrySet()) {
            String childName = childEntry.getKey();
            ModelPart childPart = childEntry.getValue();
            VanillaPart vanillaChild = childrenBacking.get(childName);
            vanillaChild.update(childPart);
        }
    }

    /**
     * Creates a part tree from the given list of roots.
     * If there's only 1 root, then it just creates the map
     * for that root. Otherwise, it creates a VanillaPart for each
     * root and has its children as the children of that root, and returns
     * a map containing those root VanillaPart instances.
     */
    public static Map<String, VanillaPart> createTreeFromRoots(List<ModelPart> roots, Map<ModelPart, VanillaPart> inverseMap) {
        List<VanillaPart> parts = new ArrayList<>(roots.size());
        for (int i = 0; i < roots.size(); i++) {
            ModelPart root = roots.get(i);
            Map<String, VanillaPart> partsForThisRoot = new HashMap<>();
            //Fill vanilla part map from the vanilla model data
            //Iterate over the root's children
            for (Map.Entry<String, ModelPart> entry : ((ModelPartAccessor) (Object) root).getChildren().entrySet()) {
                //Get the part name and value
                String partName = entry.getKey();
                ModelPart part = entry.getValue();
                //Create the new part and add it to the name->part table
                VanillaPart newPart = new VanillaPart(partName, part);
                partsForThisRoot.put(partName, newPart);
                //Populate the inverse vanilla part map
                newPart.traverse().forEach(p -> inverseMap.put(p.referencedPart, p));
            }
            parts.add(new VanillaPart("root" + (i + 1), partsForThisRoot));
        }

        //If there's only 1 root, (vast, vast majority of cases) then just return
        //its backing children map. Otherwise, create a new map which has all the roots.
        if (parts.size() == 1) {
            return parts.get(0).childrenBacking;
        } else {
            Map<String, VanillaPart> result = new HashMap<>();
            for (VanillaPart part : parts)
                result.put(part.name, part);
            return result;
        }
    }

    /**
     * Update the tree with roots. Like before, handles the cases of having one or more roots.
     */
    public static void updateTreeWithRoots(List<ModelPart> roots, Map<String, VanillaPart> partsMap, Map<ModelPart, VanillaPart> inverseMap) {
        if (roots.size() > 1) {
            for (int i = 0; i < roots.size(); i++) {
                ModelPart newRoot = roots.get(i);
                VanillaPart vanillaRoot = partsMap.get("root" + (i+1));
                //Iterate over the root's children and update the corresponding vanilla part
                for (Map.Entry<String, ModelPart> entry : ((ModelPartAccessor) (Object) newRoot).getChildren().entrySet()) {
                    String partName = entry.getKey();
                    ModelPart part = entry.getValue();
                    VanillaPart vanillaPart = vanillaRoot.children.get(partName);
                    vanillaPart.update(part);

                    //Repopulate the inverse vanilla part map
                    vanillaPart.traverse().forEach(p -> inverseMap.put(p.referencedPart, p));
                }
            }
        } else if (roots.size() == 1) {
            //If there's just one root, do not go down another layer in the tree, as it's all at the top.
            ModelPart newRoot = roots.get(0);
            for (Map.Entry<String, ModelPart> entry : ((ModelPartAccessor) (Object) newRoot).getChildren().entrySet()) {
                String partName = entry.getKey();
                ModelPart part = entry.getValue();
                VanillaPart vanillaPart = partsMap.get(partName);
                vanillaPart.update(part);

                //Repopulate the inverse vanilla part map
                vanillaPart.traverse().forEach(p -> inverseMap.put(p.referencedPart, p));
            }
        }
    }

    /**
     * The transform applied _by vanilla_ to this part, which
     * we save and can then later query from code.
     */
    public final Matrix4d savedTransform = new Matrix4d();

    /**
     * The saved vanilla part's visibility, for *reading* purposes
     */
    public boolean savedVisibility = true;

    /**
     * The inverse of the "default transform" of the model part.
     * Calculate once upon creation of the VanillaPart.
     *
     * Dust message:
     * First apply the inverse default. If the model is in its default state in Blockbench,
     * this will reverse that. For instance, it will stuff the player’s arms and neck into their body, like prewrite player models did.
     * Then apply the saved matrix. This will move the parts from this “stuffed” position into the actual, correct one.
     * This should work so long as the entity in Blockbench is shaped the same way as the “default transforms”
     * in the code set it up to be.
     * Optifine’s CEM does not obey this rule, but it’s the only sensible way to have this work, I believe.
     *
     */
    public final Matrix4f inverseDefaultTransform = new Matrix4f();

    public VanillaPart(String name, ModelPart part) {
        this.name = name;
        this.referencedPart = part;

        for (Map.Entry<String, ModelPart> child : ((ModelPartAccessor) (Object) part).getChildren().entrySet()) {
            childrenBacking.put(child.getKey(), new VanillaPart(child.getKey(), child.getValue()));
        }

        //Read the default transform and store its inverse for later
        readDefaultTransform();

        //Store the petpet field
        children = new PetPetTableView<>(childrenBacking);
    }

    private VanillaPart(String name, Map<String, VanillaPart> realChildren) {
        this.name = name;
        this.childrenBacking.putAll(realChildren);
        this.referencedPart = null;

        //Store the petpet children field
        children = new PetPetTableView<>(childrenBacking);
    }

    public Stream<VanillaPart> traverse() {
        return Stream.concat(Stream.of(this), this.childrenBacking.values().stream().flatMap(VanillaPart::traverse));
    }

    /**
     * Read the default transform, calculate its inverse, and store.
     * Some conversions are done because Minecraft's axes are a bit different
     * from Blockbench's.
     */
    private void readDefaultTransform() {
        //If there is no referenced part, the default transform is just identity
        if (referencedPart == null)
            return;

        ModelTransform defaultTransform = referencedPart.getDefaultTransform();
        float pitch = -defaultTransform.pitch;
        float yaw = -defaultTransform.yaw;
        float roll = defaultTransform.roll;

        float pivotX = -defaultTransform.pivotX;
        float pivotY = -defaultTransform.pivotY;
        float pivotZ = defaultTransform.pivotZ;

        inverseDefaultTransform.translation(pivotX / 16.0f, pivotY / 16.0f, pivotZ / 16.0f);
        if (pitch != 0.0f || yaw != 0.0f || roll != 0.0f) {
            inverseDefaultTransform.rotate(new Quaternionf().rotationZYX(roll, yaw, pitch));
        }
        inverseDefaultTransform.invert();
    }

    @PetPetWhitelist
    public VanillaPart __get_str(String s) {
        return childrenBacking.get(s);
    }

    public final PetPetTableView<String, VanillaPart> children;

    @PetPetWhitelist
    public final PetPetTableView<String, VanillaPart> children_0() {
        return children;
    }

    @PetPetWhitelist
    public String __tostring() {
        return "VanillaPart(name=" + name + ")";
    }

    @PetPetWhitelist
    public Matrix4d originMatrix_0() {
        return savedTransform;
    }

    @PetPetWhitelist
    public boolean originVisible_0() {
        return savedVisibility;
    }

    /**
     * Always recalculate matrices for vanilla parts
     */
    @Override
    public boolean forceRecalculation() {
        return true;
    }

    //Recalculation of the matrix
    //Assume that *only one thing is being rendered at a time* (one thread)
    //Otherwise, this static variable will cause bad things.
    //However, since rendering is single threaded, accessing this variable
    //statically does not cause issues.
    private static final Matrix4f tempMatrixSavedTransform = new Matrix4f();

    /**
     * First undo the "default" transformation by multiplying the inverse
     * default transform, then add the saved transform to the part.
     */
    @Override
    public boolean affectMatrix(Matrix4f matrix) {
        matrix.mul(inverseDefaultTransform);
        tempMatrixSavedTransform.set(savedTransform);
        matrix.mul(tempMatrixSavedTransform);
        return false;
    }
}
