package io.github.moonlightmaya;

import io.github.moonlightmaya.data.BaseStructures;
import io.github.moonlightmaya.model.AspectModelPart;
import io.github.moonlightmaya.model.WorldRootModelPart;
import io.github.moonlightmaya.script.AspectScriptHandler;
import io.github.moonlightmaya.script.annotations.AllowIfHost;
import io.github.moonlightmaya.script.events.EventHandler;
import io.github.moonlightmaya.texture.AspectTexture;
import io.github.moonlightmaya.util.AspectMatrixStack;
import io.github.moonlightmaya.util.EntityUtils;
import io.github.moonlightmaya.util.RenderUtils;
import io.github.moonlightmaya.vanilla.VanillaModelPartSorter;
import io.github.moonlightmaya.vanilla.VanillaRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import org.joml.Vector3d;
import petpet.external.PetPetWhitelist;
import petpet.types.PetPetTable;
import petpet.types.immutable.PetPetTableView;

import java.io.IOException;
import java.util.*;

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

    //whether the aspect is finished loading in. Some processes are asynchronous relative to the constructor
    // (namely, loading textures), so we don't want to try setting this aspect until everything is ready
    public boolean isReady;

    public final List<AspectTexture> textures;

    public final Map<String, String> scripts;

    public final VanillaRenderer vanillaRenderer;
    public final AspectScriptHandler scriptHandler;

    /**
     * The uuid of the entity using this aspect. Even if the entity itself is unloaded,
     * or has never been loaded in the first place, the aspect itself lives anyway.
     * The Aspect is attached to the UUID rather than the actual entity object.
     */
    public final UUID userUUID;
    public final UUID aspectUUID; //uuid of this aspect itself

    /**
     * Whether this aspect is "host". This term is borrowed from Figura, and I'll try to give an explanation.
     * Certain features in script are considered disallowed for Aspects other than the user's own aspects to
     * perform. There is no backend currently, but once there is, Aspects will be downloaded off the internet.
     * It's the purpose of this variable to determine if said aspects will be allowed to do certain actions,
     * which may not be desirable.
     */
    public final boolean isHost;

    /**
     * The metadata of the Aspect. Contains various useful logistical info. See class for details.
     */
    public final AspectMetadata metadata;

    /**
     * PetPet table containing the variables of the aspect.
     */
    public final PetPetTable<Object, Object> aspectVars = new PetPetTable<>();

    public Aspect(UUID userUUID, BaseStructures.AspectStructure materials) {
        this.userUUID = userUUID;
        this.aspectUUID = UUID.randomUUID();
        this.isHost = userUUID.equals(EntityUtils.getLocalUUID());

        metadata = new AspectMetadata(materials.metadata());

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
        //Set the aspect to be ready, *after* all textures finish "uploadIfNeeded()"
        RenderUtils.executeOnRenderThread(() -> isReady = true);

        //Create vanilla renderer
        vanillaRenderer = new VanillaRenderer();

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
        scriptHandler.callEvent(EventHandler.RENDER, MinecraftClient.getInstance().getTickDelta());
        matrixStack.multiply(vanillaRenderer.aspectModelTransform);
        entityRoot.render(vcp, matrixStack, light);
    }

    public UUID getAspectUUID() {
        return aspectUUID;
    }

    /**
     * When the entity first loads in, this aspect should finish creating its model.
     * The following booleans are based on:
     * - Whether the entity was ever loaded at all since this aspect was created
     * - The last known entity which used this aspect
     */
    private boolean userEverLoaded;
    private Entity user;
    private ClientWorld lastWorld; //the last known world this aspect was in
    public void tick(ClientWorld world) {
        //If the world changed, change the global var
        if (world != lastWorld) {
            scriptHandler.setGlobal("world", world);
            scriptHandler.callEvent(EventHandler.WORLD_CHANGE);
            lastWorld = world;
        }
        if (world != null) {
            if (user != null) {
                //We already know the user, and they currently exist
                //Let's see if they've unloaded:
                if (user.isRemoved() || user.world != world) {
                    //They've unloaded! Let's call the event, and set the user to null.
                    scriptHandler.callEvent(EventHandler.USER_UNLOAD);
                    scriptHandler.setGlobal("user", null);
                    user = null;
                }
            }
            if (user == null) {
                //Currently user does not exist :( check if they do now:
                Entity found = EntityUtils.getEntityByUUID(world, userUUID);
                if (found != null) {
                    //Ok, we found them! Add them to the script environment
                    //And set the user to be this entity we found with the proper uuid
                    scriptHandler.setGlobal("user", found);
                    user = found;

                    //If this is the first time the user loaded in, call the setup
                    if (!userEverLoaded) {
                        userEverLoaded = true;
                        onEntityFirstLoad(user);
                    }

                    //Either way, first time or not, let's call their user_load
                    scriptHandler.callEvent(EventHandler.USER_LOAD);
                }
            }
            if (user != null) {
                //If the user is still here at the end of it all, let's tick() them
                scriptHandler.callEvent(EventHandler.TICK);
            }

            //Always call world tick, if a world exists
            scriptHandler.callEvent(EventHandler.WORLD_TICK);
        }
    }

    /**
     * Render the world-parented parts
     */
    public void renderWorld(VertexConsumerProvider vcp, AspectMatrixStack matrixStack) {
        scriptHandler.callEvent(EventHandler.WORLD_RENDER, MinecraftClient.getInstance().getTickDelta());
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
