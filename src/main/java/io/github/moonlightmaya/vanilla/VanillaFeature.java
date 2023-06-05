package io.github.moonlightmaya.vanilla;

import io.github.moonlightmaya.model.Transformable;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import petpet.external.PetPetWhitelist;
import petpet.types.PetPetTable;

import java.util.List;
import java.util.Map;

/**
 * Analogous to the VanillaPart class -
 * while VanillaPart is Aspect's wrapper around a vanilla ModelPart object,
 * VanillaFeature is its wrapper around a vanilla FeatureRenderer object.
 *
 * This class is transformable, meaning it works similarly to model parts,
 * render tasks, and so on, with provided methods for rot, pos, scale, matrices,
 * et cetera.
 */
@PetPetWhitelist
public class VanillaFeature extends Transformable {

    /**
     * Backing variables, important data
     */
    private FeatureRenderer<?, ?> featureRenderer;

    //backing map for the model parts
    //Don't want the actual map accessible by scripts, because
    //it may mess with the data in a way we don't like
    //Instead we copy the data into a petpet table that's accessible
    private final Map<String, VanillaPart> backingParts;

    //Keep a reference to this map since we need to modify it when we update()
    private final Map<ModelPart, VanillaPart> vanillaPartInverse;

    /**
     * We also need the vanilla part inverse here because if a ModelPart
     * needs to find its corresponding vanilla part, that model part might be
     * inside of a feature renderer, but it should still be able to find the VanillaPart
     * that corresponds.
     */
    public VanillaFeature(FeatureRenderer<?, ?> featureRenderer, Map<ModelPart, VanillaPart> vanillaPartInverse) {
        this.featureRenderer = featureRenderer;
        this.vanillaPartInverse = vanillaPartInverse;

        List<ModelPart> featureRoots = EntityRendererMaps.getFeatureRoots(featureRenderer);
        this.backingParts = VanillaPart.createTreeFromRoots(featureRoots, vanillaPartInverse);
        this.parts.putAll(backingParts);
    }

    /**
     * Same as other things, when we press F3+T, feature renderers
     * are replaced by new instances, so we must update our wrappers.
     */
    public void update(FeatureRenderer<?, ?> newRenderer) {
        //Set the feature renderer
        this.featureRenderer = newRenderer;

        //Update the parts
        List<ModelPart> featureRoots = EntityRendererMaps.getFeatureRoots(newRenderer);
        VanillaPart.updateTreeWithRoots(featureRoots, this.backingParts, this.vanillaPartInverse);
    }

    /**
     * Ability to access the parts from script,
     * but not the real parts, instead a copy
     */
    private final PetPetTable<String, VanillaPart> parts = new PetPetTable<>();

    @PetPetWhitelist
    public PetPetTable<String, VanillaPart> parts_0() {
        return parts;
    }

    public String toString() {
        return "VanillaFeature";
    }

}
