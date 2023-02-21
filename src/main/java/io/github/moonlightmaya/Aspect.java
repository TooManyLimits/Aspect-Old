package io.github.moonlightmaya;

import io.github.moonlightmaya.nbt.AspectConstructionMaterials;
import io.github.moonlightmaya.nbt.NbtStructures;
import io.github.moonlightmaya.util.AspectMatrixStack;
import net.minecraft.client.render.VertexConsumerProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * An entity can equip one or more Aspects, which will alter the entity's physical appearance.
 */
public class Aspect {

    //Variables temporarily public for testing
    public AspectModelPart entityRoot;
    public List<WorldRootModelPart> worldRoots;
    public AspectModelPart skullRoot;
    public AspectModelPart hudRoot;
    public AspectModelPart portraitRoot;

    private final UUID user;

    public void renderEntity(VertexConsumerProvider vcp, AspectMatrixStack matrixStack) {
        entityRoot.render(vcp, matrixStack);
    }

    /**
     * Render the world-parented parts
     */
    public void renderWorld(VertexConsumerProvider vcp, AspectMatrixStack matrixStack) {
        for (WorldRootModelPart worldRoot : worldRoots) {
            worldRoot.render(vcp, matrixStack);
        }
    }


    public Aspect(UUID user, AspectConstructionMaterials materials) {
        this.user = user;
        entityRoot = new AspectModelPart(materials.entityRoot());
        worldRoots = new ArrayList<>(materials.worldRoots().size());
        for (NbtStructures.NbtModelPart nbtPart : materials.worldRoots())
            worldRoots.add(new WorldRootModelPart(nbtPart));
    }

}
