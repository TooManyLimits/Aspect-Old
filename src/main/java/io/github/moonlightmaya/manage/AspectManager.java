package io.github.moonlightmaya.manage;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.moonlightmaya.Aspect;
import io.github.moonlightmaya.data.BaseStructures;
import io.github.moonlightmaya.data.importing.AspectImporter;
import io.github.moonlightmaya.util.AspectMatrixStack;
import io.github.moonlightmaya.util.EntityUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
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
    }

    /**
     * Called when rendering the world. Attempts to render every single loaded Aspect's world parts.
     * @param vcp The vertex consumer provider which will be used for this rendering operation.
     * @param matrices Matrices translated to be centered at 0,0,0. This is then added to by the
     *                 world part's worldPos() vector.
     */
    public static void renderWorld(VertexConsumerProvider vcp, AspectMatrixStack matrices) {
        for (Aspect aspect : ASPECTS.values()) {
            //For each loaded aspect, render its world parts
            aspect.renderWorld(vcp, matrices);
        }
        //Also render the GUI aspect's world parts if it exists
        if (getGuiAspect() != null)
            getGuiAspect().renderWorld(vcp, matrices);
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
     * Submits a task to apply the given Aspect to
     * the given entity. The entity's previous
     * Aspect will be destroyed, if it had one.
     * This will also initialize the new Aspect.
     */
    public static void setAspect(UUID entityUUID, Aspect aspect) {
        clearAspect(entityUUID); //Clear the old aspect first
        TASKS.add(() -> {
            //Put the aspect in the map if it's ready
            if (aspect.isReady) {
                ASPECTS.put(entityUUID, aspect);
                //Also run the main script if it exists
                aspect.scriptHandler.runMain();
            } else {
                //Otherwise, try again next tick
                setAspect(entityUUID, aspect);
            }
        }); //Then apply the new aspect
    }

    /**
     * Submits a task to set the current gui aspect
     * to the provided one. If there was a previous
     * gui aspect, it is destroyed and set to null.
     */
    public static void setGuiAspect(Aspect aspect) {
        TASKS.add(() -> {
            if (currentGuiAspect != null) {
                currentGuiAspect.destroy();
                currentGuiAspect = null;
            }
            if (aspect.isReady) {
                //Set if it's ready
                currentGuiAspect = aspect;
            } else {
                //Otherwise, try again next tick
                setGuiAspect(aspect);
            }
        });
    }

    /**
     * Submits a task to remove a given entity's
     * Aspect. If it has one, it will be destroyed.
     */
    public static void clearAspect(UUID entityUUID) {
        TASKS.add(() -> {
            Aspect oldAspect = ASPECTS.remove(entityUUID);
            if (oldAspect != null) oldAspect.destroy(); //destroy the old aspect
        });
    }

    /**
     * Submits a task to clear all aspects from storage
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
    public static int cancelAspectLoading(UUID entityUUID) {
        if (!IN_PROGRESS_TIMESTAMPS.containsKey(entityUUID))
            IN_PROGRESS_TIMESTAMPS.put(entityUUID, new AtomicInteger());
        return IN_PROGRESS_TIMESTAMPS.get(entityUUID).incrementAndGet();
    }

    private static void finishLoadingTask(UUID userUUID, int requestId, Aspect aspect,
                                          Throwable error, Consumer<Throwable> errorCallback) {
        if (error == null) {
            //Check if this was the most recent request
            if (IN_PROGRESS_TIMESTAMPS.get(userUUID).get() == requestId) {
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
     * Load an aspect from a local file system folder
     */
    public static void loadAspectFromFolder(UUID userUUID, Path folder, Consumer<Throwable> errorCallback) {
        //Save my id.
        final int myId = cancelAspectLoading(userUUID);
        new AspectImporter(folder)
                .doImport() //doImport is asynchronous, so the following steps will be as well
                .thenApply(mats -> new Aspect(userUUID, mats))
                .whenComplete((aspect, error) -> finishLoadingTask(userUUID, myId, aspect, error, errorCallback));
    }

    /**
     * Load an aspect from binary data
     */
    public static void loadAspectFromData(UUID userUUID, byte[] data, Consumer<Throwable> errorCallback) {
        final int myId = cancelAspectLoading(userUUID);
        CompletableFuture.supplyAsync(() -> data)
                .thenApply(ByteArrayInputStream::new)
                .thenApply(DataInputStream::new)
                .thenApply(BaseStructures.AspectStructure::read)
                .thenApply(mats -> new Aspect(userUUID, mats))
                .whenComplete((aspect, error) -> finishLoadingTask(userUUID, myId, aspect, error, errorCallback));
    }

    //Gui aspect can only be loaded from folder
    public static void loadGuiAspect(Path folder, Consumer<Throwable> errorCallback) {
        final int myId = GUI_ASPECT_TIMESTAMP.incrementAndGet();
        new AspectImporter(folder)
                .doImport()
                .thenApply(mats -> new Aspect(null, mats))
                .whenComplete((aspect, error) -> {
                    if (error == null) {
                        if (GUI_ASPECT_TIMESTAMP.get() == myId) {
                            setGuiAspect(aspect);
                            return;
                        }
                        aspect.destroy();
                    } else {
                        errorCallback.accept(error.getCause());
                    }
                });
    }

}
