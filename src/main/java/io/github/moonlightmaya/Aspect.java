package io.github.moonlightmaya;

import io.github.moonlightmaya.data.BaseStructures;
import io.github.moonlightmaya.model.AspectModelPart;
import io.github.moonlightmaya.model.WorldRootModelPart;
import io.github.moonlightmaya.texture.AspectTexture;
import io.github.moonlightmaya.util.AspectMatrixStack;
import io.github.moonlightmaya.util.RenderUtils;
import io.github.moonlightmaya.vanilla.VanillaModelPartSorter;
import io.github.moonlightmaya.vanilla.VanillaRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.entity.Entity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * An entity can equip one or more Aspects, which will alter the entity's physical appearance.
 */
public class Aspect {

    //Variables temporarily public for testing
    public final AspectModelPart entityRoot;
    public final List<WorldRootModelPart> worldRoots;
    public final List<AspectTexture> textures;

    public final VanillaRenderer vanillaRenderer = new VanillaRenderer();

    public final Entity user; //the entity using this aspect
    public final UUID aspectId; //uuid of this aspect itself

    public void renderEntity(VertexConsumerProvider vcp, AspectMatrixStack matrixStack, int light) {
        matrixStack.multiply(vanillaRenderer.aspectModelTransform);
        entityRoot.render(vcp, matrixStack, light);
    }

    public UUID getAspectId() {
        return aspectId;
    }

    /**
     * Render the world-parented parts
     */
    public void renderWorld(VertexConsumerProvider vcp, AspectMatrixStack matrixStack) {
        for (WorldRootModelPart worldRoot : worldRoots) {
            worldRoot.render(vcp, matrixStack);
        }
    }


    public Aspect(Entity user, BaseStructures.AspectStructure materials) {
        this.user = user;
        this.aspectId = UUID.randomUUID();

        //Load textures first, needed for making model parts
        textures = new ArrayList<>();
        for (BaseStructures.Texture base : materials.textures()) {
            try {
                AspectTexture tex = new AspectTexture(this, base);
                tex.uploadIfNeeded(); //upload texture
                textures.add(tex);
            } catch (IOException e) {
                throw new RuntimeException("Error importing texture " + base.name() + "!");
            }
        }

        //Also grab vanilla model data for the entity
        EntityModel<?> model = RenderUtils.getModel(user);
        if (model != null) {
            vanillaRenderer.initVanillaParts(VanillaModelPartSorter.getModelInfo(model));
        }

        entityRoot = new AspectModelPart(materials.entityRoot(), this);
        worldRoots = new ArrayList<>(materials.worldRoots().size());

        for (BaseStructures.ModelPartStructure worldRoot : materials.worldRoots())
            worldRoots.add(new WorldRootModelPart(worldRoot, this));
    }

    /**
     * Destroy this object and free any native resources.
     */
    public void destroy() {

    }

}
