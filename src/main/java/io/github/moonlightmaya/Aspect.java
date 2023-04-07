package io.github.moonlightmaya;

import io.github.moonlightmaya.data.BaseStructures;
import io.github.moonlightmaya.model.AspectModelPart;
import io.github.moonlightmaya.model.WorldRootModelPart;
import io.github.moonlightmaya.script.AspectScriptHandler;
import io.github.moonlightmaya.texture.AspectTexture;
import io.github.moonlightmaya.util.AspectMatrixStack;
import io.github.moonlightmaya.util.RenderUtils;
import io.github.moonlightmaya.vanilla.VanillaModelPartSorter;
import io.github.moonlightmaya.vanilla.VanillaRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.entity.Entity;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.function.Supplier;

/**
 * An entity can equip one or more Aspects, which will alter the entity's physical appearance.
 */
public class Aspect {

    /**
     * The roots of the different model part instances, as well as their temporarily held data.
     * The data is saved from when the Aspect instance is first constructed. Then, when the user of the
     * Aspect loads in for the first time, the data is converted into the model parts themselves, and the
     * data is discarded.
     */
    public AspectModelPart entityRoot; private BaseStructures.ModelPartStructure entityRootData;
    public List<WorldRootModelPart> worldRoots;


    public final List<AspectTexture> textures;

    public final Map<String, String> scripts;

    public final VanillaRenderer vanillaRenderer = new VanillaRenderer();
    public final AspectScriptHandler scriptHandler;

    //The uuid of the entity using this aspect. Even if the entity itself is unloaded, the aspect itself lives on,
    //and the Aspect is attached to the UUID rather than the actual entity.
    public final UUID userUUID;

    public final UUID aspectId; //uuid of this aspect itself

    public Aspect(UUID userUUID, BaseStructures.AspectStructure materials) {
        this.userUUID = userUUID;
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

        //Save the entity root data
        entityRootData = materials.entityRoot();

        //World roots don't need the entity itself. Since they render as part of the world,
        //They cannot interact with vanilla model parents (at least through parent types).
        worldRoots = new ArrayList<>(materials.worldRoots().size());
        for (BaseStructures.ModelPartStructure worldRoot : materials.worldRoots())
            worldRoots.add(new WorldRootModelPart(worldRoot, this));

        //Separate out the list of script objects into a map instead
        //Names are keys, source is values
        scripts = new HashMap<>();
        for (BaseStructures.Script script : materials.scripts())
            scripts.put(script.name(), script.source());

        scriptHandler = new AspectScriptHandler(this);
    }

    /**
     * When the entity first loads in, this aspect should finish creating its model.
     * The following booleans are based on:
     * - Whether the entity was loaded last tick
     * - Whether the entity was ever loaded before
     */
    public boolean entityWasLoaded;
    public boolean entityEverLoaded;
    public void onEntityFirstLoad(Entity user) {
        //Generate vanilla data for parent part purposes
        EntityModel<?> model = RenderUtils.getModel(user);
        if (model != null) {
            vanillaRenderer.initVanillaParts(VanillaModelPartSorter.getModelInfo(model));
        }

        //Discard the data after, no longer needed
        entityRoot = new AspectModelPart(entityRootData, this, null);
        entityRootData = null;

        //Notify the script
        scriptHandler.onEntityFirstLoad();
    }

    public void renderEntity(VertexConsumerProvider vcp, AspectMatrixStack matrixStack, int light) {
        scriptHandler.callEvent("render", MinecraftClient.getInstance().getTickDelta());
        matrixStack.multiply(vanillaRenderer.aspectModelTransform);
        entityRoot.render(vcp, matrixStack, light);
    }

    public UUID getAspectId() {
        return aspectId;
    }

    /**
     * Runs the main script if it exists
     */
    public void runScript() {
        if (scripts.size() > 0) {
            scriptHandler.runMain();
        }
    }

    /**
     * Render the world-parented parts
     */
    public void renderWorld(VertexConsumerProvider vcp, AspectMatrixStack matrixStack) {
        for (WorldRootModelPart worldRoot : worldRoots) {
            worldRoot.render(vcp, matrixStack);
        }
    }

    /**
     * Destroy this object and free any native resources.
     */
    public void destroy() {
        //TODO
    }

}
