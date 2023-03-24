package io.github.moonlightmaya.manage;

import io.github.moonlightmaya.Aspect;
import io.github.moonlightmaya.data.BaseStructures;
import io.github.moonlightmaya.data.importing.AspectImporter;
import io.github.moonlightmaya.util.DisplayUtils;
import net.minecraft.client.MinecraftClient;

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
    private static final Map<UUID, Aspect> ENTITY_ASPECTS = new HashMap<>();

    /**
     * A queue of all tasks which need to be done relating to avatar management.
     * Each task is a subclass of AspectManagementTask, viewable in that class.
     * There will be helper methods in this class to submit certain tasks to the queue.
     *
     * SHOULD NOT modify the Aspect maps outside of processing tasks!
     * This is a recipe for synchronization issues.
     */
    private static final ConcurrentLinkedQueue<Runnable> TASKS = new ConcurrentLinkedQueue<>();

    //Global tick method for the aspect manager. Called each client tick.
    public static void tick(MinecraftClient client) {
        //Each tick, perform tasks waiting in the queue.
        while (!TASKS.isEmpty()) {
            TASKS.poll().run(); //Poll the task and run it
        }
    }

    public static void setAspect(UUID entityUUID, Aspect aspect) {
        TASKS.add(() -> ENTITY_ASPECTS.put(entityUUID, aspect));
    }
    public static void clearAspect(UUID entityUUID) {
        TASKS.add(() -> ENTITY_ASPECTS.remove(entityUUID));
    }

    private static final Map<UUID, AtomicInteger> IN_PROGRESS_TIMESTAMPS = new ConcurrentHashMap<>();

    /**
     * Cancel the loading of the current aspect for this entity
     * This doesn't actually stop the loading operation, which would
     * be unsafe and may leak native resources. However, it means that
     * when the aspect finishes, it will not be set as this entity's
     * avatar, and will instead be safely destroyed.
     *
     * Returns the new id, for use in internal functions. Functions
     * outside this class shouldn't care about the return value.
     */
    public static int cancelAspectLoading(UUID entityUUID) {
        if (!IN_PROGRESS_TIMESTAMPS.containsKey(entityUUID))
            IN_PROGRESS_TIMESTAMPS.put(entityUUID, new AtomicInteger());
        return IN_PROGRESS_TIMESTAMPS.get(entityUUID).incrementAndGet();
    }

    private static void finishLoadingTask(UUID entityUUID, int requestId, Aspect aspect,
                                          Throwable error, Consumer<Throwable> errorCallback) {
        if (error == null) {
            //Check if this was the most recent request
            if (IN_PROGRESS_TIMESTAMPS.get(entityUUID).get() == requestId) {
                //If so, then submit the task to set aspect:
                setAspect(entityUUID, aspect);
            }
            //Otherwise, this request is outdated. Destroy the aspect
            //and do not set it.
            aspect.destroy();
        } else {
            //If did not complete, report error
            errorCallback.accept(error);
        }
    }

    //Load an aspect from a local file system folder
    public static void loadAspectFromFolder(UUID entityUUID, Path folder, Consumer<Throwable> errorCallback) {
        //Save my id.
        final int myId = cancelAspectLoading(entityUUID);
        new AspectImporter(folder)
                .doImport()
                .thenApplyAsync(mats -> new Aspect(entityUUID, mats))
                .whenCompleteAsync((aspect, error) -> finishLoadingTask(entityUUID, myId, aspect, error, errorCallback));
    }

    //Load an aspect from binary data
    public static void loadAspectFromData(UUID entityUUID, byte[] data, Consumer<Throwable> errorCallback) {
        final int myId = cancelAspectLoading(entityUUID);
        CompletableFuture.supplyAsync(() -> data)
                .thenApply(ByteArrayInputStream::new)
                .thenApply(DataInputStream::new)
                .thenApplyAsync(BaseStructures.AspectStructure::read)
                .thenApplyAsync(mats -> new Aspect(entityUUID, mats))
                .whenCompleteAsync((aspect, error) -> finishLoadingTask(entityUUID, myId, aspect, error, errorCallback));
    }

}
