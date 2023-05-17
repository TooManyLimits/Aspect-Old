package io.github.moonlightmaya.manage;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.moonlightmaya.Aspect;
import io.github.moonlightmaya.game_interfaces.AspectConfig;
import io.github.moonlightmaya.manage.data.BaseStructures;
import io.github.moonlightmaya.manage.data.importing.AspectImporter;
import io.github.moonlightmaya.model.AspectTexture;
import io.github.moonlightmaya.util.AspectMatrixStack;
import io.github.moonlightmaya.util.DisplayUtils;
import io.github.moonlightmaya.util.IOUtils;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.world.ClientWorld;
import org.jetbrains.annotations.Nullable;
import petpet.lang.run.PetPetException;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Static class, the global aspect manager deals with
 * the flow and assignment of aspects.
 * It keeps track of which entities have which aspects,
 * which aspects are currently being loaded, and so on.
 */
public class AspectManager {


    /**
     * Global map from entity UUIDs to complete Aspect instances.
     */
    private static final Map<UUID, Aspect> ASPECTS = new HashMap<>();

    /**
     * The currently selected GUI Aspect
     */
    private static Aspect currentGuiAspect = null;

    /**
     * A queue of all tasks which need to be done relating to aspect management.
     * There will be helper methods in this class to submit certain tasks to the queue.
     *
     * SHOULD NOT modify the Aspect maps outside of processing tasks!
     * This is a recipe for synchronization issues.
     */
    private static final ConcurrentLinkedQueue<Runnable> TASKS = new ConcurrentLinkedQueue<>();

    /**
     * Global tick method for the aspect manager. Called each client tick.
     */
    public static void tick(ClientWorld world) {
        assert RenderSystem.isOnRenderThreadOrInit(); //assertion to hopefully avoid some annoying threading issues

        //Each tick, perform tasks waiting in the queue.
        //Reason for "for" loop instead of "while" is that some tasks
        //may add new ones to the queue, to be processed on the next tick,
        // for example setAspect if the aspect isn't ready yet
        int numTasks = TASKS.size();
        for (int i = 0; i < numTasks; i++) {
            TASKS.poll().run(); //Poll the task and run it
        }

        //Tick each Aspect
        for (Aspect aspect : ASPECTS.values())
            aspect.tick(world);
        if (currentGuiAspect != null)
            currentGuiAspect.tick(world);
    }

    /**
     * Called when rendering the world. Attempts to render every single loaded Aspect's world parts.
     * @param vcp The vertex consumer provider which will be used for this rendering operation.
     * @param matrices Matrices translated to be centered at 0,0,0. This is then added to by the
     *                 world part's worldPos() vector.
     */
    public static void renderWorld(VertexConsumerProvider vcp, float tickDelta, AspectMatrixStack matrices) {
        for (Aspect aspect : ASPECTS.values()) {
            //For each loaded aspect, ensure its textures are uploaded
            for (AspectTexture tex : aspect.textures)
                tex.uploadIfNeeded();

            //And render the aspect's world parts
            aspect.renderWorld(vcp, tickDelta, matrices);
        }
        //Also render the GUI aspect's world parts if it exists
        if (getGuiAspect() != null)
            getGuiAspect().renderWorld(vcp, tickDelta, matrices);
    }

    /**
     * Returns the aspect of the given entity.
     * If the entity has no equipped aspect, returns null.
     */
    @Nullable
    public static Aspect getAspect(UUID uuid) {
        return ASPECTS.get(uuid);
    }

    @Nullable
    public static Aspect getGuiAspect() {
        return currentGuiAspect;
    }

    /**
     * Reloads the gui aspect using the current config setting
     */
    public static void reloadGuiAspect() {
        Consumer<Throwable> reporter = t -> DisplayUtils.displayError("Failed to load GUI aspect", t, true);
        try {
            String relPath = AspectConfig.GUI_PATH.get();
            if (relPath.length() == 0) {
                //Default GUI
                try(InputStream in = IOUtils.getAsset("aspects/gui.aspect")) {
                    if (in == null)
                        throw new IOException("Unable to get default GUI aspect");
                    byte[] data = in.readAllBytes();
                    AspectManager.loadAspectFromData(null, data, reporter, true, true);
                }
            } else {
                Path path = IOUtils.getOrCreateModFolder().resolve("guis").resolve(relPath);
                AspectManager.loadAspectFromPath(null, path, reporter, true, true);
            }
        } catch (Throwable t) {
            reporter.accept(t);
        }
    }

    /**
     * Submits a task to apply the given Aspect to
     * the given entity. The entity's previous
     * Aspect will be destroyed, if it had one.
     * This will also initialize the new Aspect.
     */
    public static void setAspect(@Nullable UUID entityUUID, Aspect aspect) {
        clearAspect(entityUUID); //Clear the old aspect first
        TASKS.add(() -> {
            //Put the aspect in the map if it's ready
            if (aspect.isReady) {
                if (entityUUID == null) { //Gui aspect
                    currentGuiAspect = aspect;
                } else {
                    ASPECTS.put(entityUUID, aspect);
                }
                //Also run the main script if it exists
                aspect.scriptHandler.runMain();
            } else {
                //Otherwise, try again next tick
                setAspect(entityUUID, aspect);
            }
        }); //Then apply the new aspect
    }

    /**
     * Submits a task to remove a given entity's
     * Aspect. If it has one, it will be destroyed.
     */
    public static void clearAspect(@Nullable UUID entityUUID) {
        //If null, clear gui aspect, otherwise clear regular one
        TASKS.add(() -> {
            Aspect oldAspect;
            if (entityUUID == null) { //Gui aspect
                oldAspect = currentGuiAspect;
                currentGuiAspect = null;
            } else {
                oldAspect = ASPECTS.remove(entityUUID);
            }
            if (oldAspect != null)
                oldAspect.destroy();
        });
    }

    /**
     * Submits a task to clear all aspects from storage, but not the GUI aspect.
     */
    public static void clearAllAspects() {
        TASKS.add(() -> {
            for (Aspect aspect : ASPECTS.values())
                aspect.destroy();
            ASPECTS.clear();
        });
    }

    /**
     * The reason for this timestamp has to do with long-running load operations.
     * Imagine someone accidentally attempts to load a large aspect that takes a
     * long time to complete, then clicks the aspect they actually wanted, which
     * loads nearly instantly. We need a way for the long-running task to know
     * that it isn't the most recent request anymore, and to discard its result
     * once it completes. This atomic counter handles that.
     */
    private static final Map<UUID, AtomicInteger> IN_PROGRESS_TIMESTAMPS = new ConcurrentHashMap<>();
    private static final AtomicInteger GUI_ASPECT_TIMESTAMP = new AtomicInteger();

    /**
     * Cancel the loading of the current aspect for this entity
     * This doesn't actually stop the loading operation, which would
     * be unsafe and may leak native resources. However, it means that
     * when the aspect finishes, it will NOT be set on this entity,
     * and will instead be safely destroyed.
     *
     * Returns the new id, for use in internal functions. Functions
     * outside this class shouldn't care about the return value.
     */
    public static int cancelAspectLoading(@Nullable UUID entityUUID) {
        if (entityUUID == null) //gui aspect special case
            return GUI_ASPECT_TIMESTAMP.incrementAndGet();
        if (!IN_PROGRESS_TIMESTAMPS.containsKey(entityUUID))
            IN_PROGRESS_TIMESTAMPS.put(entityUUID, new AtomicInteger());
        return IN_PROGRESS_TIMESTAMPS.get(entityUUID).incrementAndGet();
    }

    private static void finishLoadingTask(@Nullable UUID userUUID, int requestId, Aspect aspect,
                                          Throwable error, Consumer<Throwable> errorCallback) {
        if (error == null) {
            //Check if this was the most recent request
            AtomicInteger i = userUUID == null ? GUI_ASPECT_TIMESTAMP : IN_PROGRESS_TIMESTAMPS.get(userUUID);
            if (i.get() == requestId) {
                //If so, then submit the task to set aspect:
                setAspect(userUUID, aspect);
                return;
            }
            //Otherwise, this request is outdated. Destroy the aspect
            //and do not set it.
            aspect.destroy();
        } else {
            //If did not complete, report error
            //Error is a weird CompleteableFuture error that wraps the real error, so get cause
            errorCallback.accept(error.getCause());
        }
    }

    /**
     * Load an aspect from a local file system path
     * If the path is a `.aspect` file, then load it from the data
     * If the path is a folder, treat it as a folder
     */
    public static void loadAspectFromPath(@Nullable UUID userUUID, Path folder, Consumer<Throwable> errorCallback, boolean isHost, boolean isGui) {
        //Save my id.
        assert !isGui || isHost && userUUID == null;

        if (folder.toFile().isDirectory()) {
            final int myId = cancelAspectLoading(userUUID);
            new AspectImporter(folder)
                    .doImport() //doImport is asynchronous, so the following steps will be as well
                    .thenApply(mats -> new Aspect(userUUID, mats, isHost, isGui))
                    .whenComplete((aspect, error) -> finishLoadingTask(userUUID, myId, aspect, error, errorCallback));
        } else if (folder.toFile().exists() && folder.toFile().getName().endsWith(".aspect")) {
            try {
                byte[] bytes = Files.readAllBytes(folder);
                loadAspectFromData(userUUID, bytes, errorCallback, isHost, isGui);
            } catch (Exception e) {
                throw new PetPetException("Failed to read bytes at " + folder, e);
            }
        } else {
            throw new PetPetException("Path " + folder + " does not exist or is not a valid aspect path");
        }
    }

    /**
     * Load an aspect from binary data
     */
    public static void loadAspectFromData(@Nullable UUID userUUID, byte[] data, Consumer<Throwable> errorCallback, boolean isHost, boolean isGui) {
        assert !isGui || isHost && userUUID == null;
        final int myId = cancelAspectLoading(userUUID);
        CompletableFuture.supplyAsync(() -> data)
                .thenApply(ByteArrayInputStream::new)
                .thenApply(DataInputStream::new)
                .thenApply(BaseStructures.AspectStructure::read)
                .thenApply(mats -> new Aspect(userUUID, mats, isHost, isGui))
                .whenComplete((aspect, error) -> finishLoadingTask(userUUID, myId, aspect, error, errorCallback));
    }

}
