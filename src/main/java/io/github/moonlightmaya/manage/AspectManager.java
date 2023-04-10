package io.github.moonlightmaya.manage;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.moonlightmaya.Aspect;
import io.github.moonlightmaya.data.BaseStructures;
import io.github.moonlightmaya.data.importing.AspectImporter;
import io.github.moonlightmaya.util.EntityUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
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
     * A queue of all tasks which need to be done relating to avatar management.
     * There will be helper methods in this class to submit certain tasks to the queue.
     *
     * SHOULD NOT modify the Aspect maps outside of processing tasks!
     * This is a recipe for synchronization issues.
     */
    private static final ConcurrentLinkedQueue<Runnable> TASKS = new ConcurrentLinkedQueue<>();

    /**
     * Global tick method for the aspect manager. Called each client tick.
     */
    public static void tick(MinecraftClient client) {
        assert RenderSystem.isOnRenderThreadOrInit(); //assertion to hopefully avoid some annoying threading issues

        //Each tick, perform tasks waiting in the queue.
        while (!TASKS.isEmpty()) {
            TASKS.poll().run(); //Poll the task and run it
        }

        //Tick each Aspect
        for (Aspect aspect : ASPECTS.values())
            aspect.tick();
    }

    /**
     * Returns the aspect of the given entity.
     * If the entity has no equipped aspect, returns null.
     */
    @Nullable
    public static Aspect getAspect(UUID uuid) {
        return ASPECTS.get(uuid);
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
            //Put the aspect in the map
            ASPECTS.put(entityUUID, aspect);
            //Also run the main script if it exists
            aspect.scriptHandler.runMain();
        }); //Then apply the new aspect
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

}
