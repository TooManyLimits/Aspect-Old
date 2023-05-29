package io.github.moonlightmaya.script.vanilla;

import io.github.moonlightmaya.model.Transformable;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import petpet.external.PetPetWhitelist;
import petpet.types.PetPetList;
import petpet.types.immutable.PetPetListView;

import java.util.List;

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
    private final List<VanillaPart> parts = new PetPetList<>();

    public VanillaFeature(FeatureRenderer<?, ?> featureRenderer) {
        this.featureRenderer = featureRenderer;
    }

    /**
     * Same as other things, when we press F3+T, feature renderers
     * are replaced by new instances, so we must update our wrappers.
     */
    public void update(FeatureRenderer<?, ?> newRenderer) {
        parts.clear();
        this.featureRenderer = newRenderer;
    }

    /**
     * Ability to access the parts from script
     */
    @PetPetWhitelist
    public PetPetListView<VanillaPart> parts_0() {
        return new PetPetListView<>(parts);
    }

    public String toString() {
        return "VanillaFeature";
    }

}
