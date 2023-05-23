package io.github.moonlightmaya;

import io.github.moonlightmaya.manage.AspectMetadata;
import io.github.moonlightmaya.manage.data.BaseStructures;
import io.github.moonlightmaya.model.AspectModelPart;
import io.github.moonlightmaya.model.WorldRootModelPart;
import io.github.moonlightmaya.script.AspectScriptHandler;
import io.github.moonlightmaya.script.events.EventHandler;
import io.github.moonlightmaya.model.AspectTexture;
import io.github.moonlightmaya.util.AspectMatrixStack;
import io.github.moonlightmaya.util.DisplayUtils;
import io.github.moonlightmaya.util.EntityUtils;
import io.github.moonlightmaya.util.RenderUtils;
import io.github.moonlightmaya.script.vanilla.VanillaModelPartSorter;
import io.github.moonlightmaya.script.vanilla.VanillaRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import org.jetbrains.annotations.Nullable;
import petpet.types.PetPetTable;

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
    public AspectModelPart hudRoot;

    //whether the aspect is finished loading in. Some processes are asynchronous relative to the constructor
    // (namely, loading textures), so we don't want to try setting this aspect until everything is ready
    public boolean isReady;

    public final List<AspectTexture> textures;

    public final Map<String, String> scripts;

    public final VanillaRenderer vanillaRenderer;
    public final AspectScriptHandler scriptHandler;

    private Throwable error;
    private ErrorLocation errorLocation;

    /**
     * The context in which this aspect is being rendered.
     * If vanilla minecraft is drawing the entity, then it's
     * specified, but if another mod (like iris's shadow pass)
     * is drawing the entity, we don't know and just say OTHER.
     */
    public String renderContext = RenderContexts.OTHER;

    /**
     * The uuid of the entity using this aspect. Even if the entity itself is unloaded,
     * or has never been loaded in the first place, the aspect itself lives anyway.
     * The Aspect is attached to the UUID rather than the actual entity object.
     */
    public final @Nullable UUID userUUID;
    public final UUID aspectUUID; //uuid of this aspect itself. not used for much

    /**
     * Whether this aspect is "host". This term is borrowed from Figura, and I'll try to give an explanation.
     * Certain features in script are considered disallowed for Aspects other than the user's own aspects to
     * perform. There is no backend currently, but once there is, Aspects will be downloaded off the internet.
     * It's the purpose of this variable to determine if said aspects will be allowed to do certain actions,
     * which may not be desirable.
     *
     * isGui performs a similar role, except GUI aspects have even greater permissions than host ones.
     */
    public final boolean isHost;
    public final boolean isGui;

    /**
     * The metadata of the Aspect. Contains various useful logistical info. See class for details.
     */
    public final AspectMetadata metadata;

    /**
     * PetPet table containing the variables of the aspect.
     */
    public final PetPetTable<Object, Object> aspectVars = new PetPetTable<>();

    public Aspect(@Nullable UUID userUUID, BaseStructures.AspectStructure materials, boolean isHost, boolean isGui) {
        this.userUUID = userUUID;
        this.aspectUUID = UUID.randomUUID();
        this.isHost = isHost;
        this.isGui = isGui;

        metadata = new AspectMetadata(materials.metadata());

        //Load textures first, needed for making model parts
        textures = new ArrayList<>();
        for (BaseStructures.Texture base : materials.textures()) {
            try {
                AspectTexture tex = new AspectTexture(this, base);
                tex.uploadIfNeeded(); //upload texture
                textures.add(tex);
            } catch (IOException e) {
                RuntimeException re = new RuntimeException("Error importing texture " + base.name() + "!", e);
                //error(re, ErrorLocation.LOAD);
                throw re;
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

        //Hud root
        hudRoot = new AspectModelPart(materials.hudRoot(), this, null);

        //Separate out the list of script objects into a map instead
        //Names are keys, source is values
        scripts = new HashMap<>();
        for (BaseStructures.Script script : materials.scripts())
            scripts.put(script.name(), script.source());

        scriptHandler = new AspectScriptHandler(this);
    }


    public void onEntityFirstLoad(Entity user) {
        if (isErrored()) return;
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

    public void renderEntity(VertexConsumerProvider vcp, float tickDelta, AspectMatrixStack matrixStack, int light, int overlay) {
        if (isErrored()) return;
        scriptHandler.callEvent(EventHandler.RENDER, tickDelta, renderContext);
        matrixStack.multiply(vanillaRenderer.aspectModelTransform);
        try {
            entityRoot.render(vcp, matrixStack, light, overlay);
        } catch (Throwable t) {
            error(t, ErrorLocation.RENDER_ENTITY);
        }
        scriptHandler.callEvent(EventHandler.POST_RENDER, tickDelta, renderContext);
        //After rendering, reset the render context to "other"
        renderContext = RenderContexts.OTHER;
    }

    public void renderHud(VertexConsumerProvider vcp, float tickDelta, AspectMatrixStack matrixStack) {
        if (isErrored()) return;
        scriptHandler.callEvent(EventHandler.HUD_RENDER, tickDelta);
        try {
            hudRoot.render(vcp, matrixStack, LightmapTextureManager.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV);
        } catch (Throwable t) {
            error(t, ErrorLocation.RENDER_HUD);
        }
        scriptHandler.callEvent(EventHandler.POST_HUD_RENDER, tickDelta);
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
        if (isErrored()) return;
        try {
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
                    //Gui aspects' "user" is the local player
                    Entity found = isGui ?
                            MinecraftClient.getInstance().player :
                            EntityUtils.getEntityByUUID(world, userUUID);

                    //Currently user does not exist :( check if they do now:
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
        } catch (Throwable t) {
            error(t, ErrorLocation.TICK);
        }
    }

    /**
     * Render the world-parented parts
     */
    public void renderWorld(VertexConsumerProvider vcp, float tickDelta, AspectMatrixStack matrixStack) {
        if (isErrored()) return;
        scriptHandler.callEvent(EventHandler.WORLD_RENDER, tickDelta, renderContext);
        try {
            for (WorldRootModelPart worldRoot : worldRoots) {
                worldRoot.render(vcp, matrixStack);
            }
        } catch (Throwable t) {
            error(t, ErrorLocation.RENDER_WORLD);
        }
        scriptHandler.callEvent(EventHandler.POST_WORLD_RENDER, tickDelta, renderContext);
        //After rendering the world, reset my context to other
        this.renderContext = RenderContexts.OTHER;
    }

    /**
     * Destroy this object and free any native resources.
     * - Close all the textures
     */
    public void destroy() {
        for (AspectTexture texture : textures)
            texture.close();
    }

    public boolean isErrored() {
        return error != null;
    }

    public void error(Throwable t, ErrorLocation location) {
        this.error = t;
        this.errorLocation = location;
        if (isHost) {
            DisplayUtils.displayError("Equipped aspect errored in " + location.name(), error, true);
        }
    }

    /**
     * The possible locations of an error happening
     */
    public enum ErrorLocation {
        SCRIPT,
        RENDER_ENTITY,
        RENDER_WORLD,
        RENDER_HUD,
        TICK,
        LOAD
    }

    /**
     * These are strings instead of enums in the event
     * of custom render contexts being created.
     */
    public static class RenderContexts {
        public static final String
                WORLD = "WORLD", //Aspect is rendered normally in the minecraft world
                MINECRAFT_GUI = "MINECRAFT_GUI", //Aspect is rendered in the minecraft inventory screen or other screens
                OTHER = "OTHER"; //Aspect is rendered in some other circumstance, perhaps added by another mod
    }

}
