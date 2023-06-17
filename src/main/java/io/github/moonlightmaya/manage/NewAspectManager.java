package io.github.moonlightmaya.manage;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.moonlightmaya.Aspect;
import io.github.moonlightmaya.manage.data.BaseStructures;
import io.github.moonlightmaya.manage.data.importing.AspectImporter;
import io.github.moonlightmaya.util.AspectMatrixStack;
import io.github.moonlightmaya.util.DisplayUtils;
import io.github.moonlightmaya.util.IOUtils;
import io.github.moonlightmaya.util.RenderUtils;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.world.ClientWorld;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * The new manager for Aspect instances, since the last one
 * got too overcomplicated and weird.
 */
public class NewAspectManager {

    public static final SubManager<UUID> ENTITIES = new SubManager<>();
    public static final SubManager<GuiType> GUI = new SubManager<>();
    public static final SubManager<String> NAMED = new SubManager<>();

    /**
     * Each tick, this is called. The TASKS queue is polled, and waiting tasks
     * are executed. Tasks in the queue should be small and quick to execute,
     * to ensure that there isn't tons of slowdown.
     */
    public static void tick(ClientWorld world) {
        assert RenderSystem.isOnRenderThreadOrInit(); //Bug net

        ENTITIES.tick(world);
        GUI.tick(world);
        NAMED.tick(world);
    }

    /**
     * Called when rendering the world. Attempts to render every single loaded Aspect's world parts.
     * @param vcp The vertex consumer provider which will be used for this rendering operation.
     * @param matrices Matrices translated to be centered at 0,0,0. This is then added to by the
     *                 world part's worldPos() vector.
     */
    public static void worldRender(VertexConsumerProvider vcp, float tickDelta, AspectMatrixStack matrices) {
        //Update the world to view matrices using the matrix stack info
        RenderUtils.updateWorldViewMatrices(matrices.peekPosition());

        //The render context for all parts is currently "WORLD".
        //In the future, it may be necessary to pass in a context to this method
        //in the event that we want world parts to render with Iris shaders (which we do)
        ENTITIES.worldRender(vcp, tickDelta, matrices);
        NAMED.worldRender(vcp, tickDelta, matrices);
        GUI.worldRender(vcp, tickDelta, matrices);
    }

    public enum GuiType {
        ASPECT_GUI
    }

    /**
     * Manages Aspects connected to a single type T.
     * Most commonly used T is UUID, for entities.
     * In the future we may expand this.
     */
    private static class SubManager<T> {
        private final ConcurrentMap<T, Aspect> loadedAspects = new ConcurrentHashMap<>();
        private final ConcurrentMap<T, CompletableFuture<Aspect>> inProgressAspects = new ConcurrentHashMap<>();

        private void tick(ClientWorld world) {
            //Check on the in progress aspects
            for (Map.Entry<T, CompletableFuture<Aspect>> entry : inProgressAspects.entrySet()) {
                T id = entry.getKey();
                CompletableFuture<Aspect> future = entry.getValue();
                if (future.isDone()) {
                    inProgressAspects.remove(id);
                    handleCompletedFuture(id, future);
                }
            }

            //Tick the completed aspects
            for (Aspect aspect : loadedAspects.values())
                aspect.tick(world);
        }

        private void handleCompletedFuture(T id, CompletableFuture<Aspect> completed) {
            if (completed.isCompletedExceptionally()) {
                //why is there no getException() :sob:
                Throwable cause = null;
                try {
                    completed.getNow(null);
                } catch (Throwable t) {
                    //CompletableFuture wraps exceptions inside their own exception, so get the cause
                    cause = t.getCause();
                }

                //For now, just always report errors to chat
                DisplayUtils.displayError("Failed to load aspect", cause, true);
            } else {
                Aspect a = completed.getNow(null);
                if (a == null) throw new IllegalStateException("Aspect loading said it was done, but it isn't?");
                set(id, a);
            }
        }

        private void set(T id, Aspect aspect) {
            Aspect old = loadedAspects.put(id, aspect);
            if (old != null)
                old.destroy();
        }

        public void clearAll() {
            for (T id : inProgressAspects.keySet())
                cancel(id);
            inProgressAspects.clear();
            for (Aspect aspect : loadedAspects.values())
                aspect.destroy();
            loadedAspects.clear();
        }

        /**
         * Clear the Aspect from this object
         * and cancel any in-progress task.
         */
        public void clear(T id) {
            cancel(id);
            Aspect old = loadedAspects.get(id);
            if (old != null)
                old.destroy();
        }

        /**
         * Cancel any in-progress loading aspect for this object.
         * This does not actually cancel the completable future;
         * if we accidentally GC an Aspect without first calling
         * destroy() on it, then memory will leak. So instead of
         * actually cancelling it, we just remove it from the map,
         * and tell it to destroy the aspect once it's done.
         */
        public void cancel(T id) {
            //Remove the in progress aspect from the set, and make it
            //destroy its aspect once it's done
            CompletableFuture<Aspect> oldAspectTask = inProgressAspects.remove(id);
            if (oldAspectTask != null)
                oldAspectTask.thenAccept(Aspect::destroy);
        }

        public void load(T id, byte[] data, Aspect.ApiAccessLevel accessLevel) {
            CompletableFuture<Aspect> future = CompletableFuture.supplyAsync(IOUtils.wrapExcepting(() -> {
                BaseStructures.AspectStructure base = BaseStructures.AspectStructure.read(new DataInputStream(new ByteArrayInputStream(data)));
                //Todo: Aspects that aren't necessarily on entities or having a "user uuid"
                return new Aspect(id, base, accessLevel);
            }, CompletionException::new));
            cancel(id); //Cancel any in progress one that already exists
            inProgressAspects.put(id, future);
        }

        public void load(T id, Path localPath, Aspect.ApiAccessLevel accessLevel) {
            CompletableFuture<Aspect> future = CompletableFuture.supplyAsync(IOUtils.wrapExcepting(() -> {
                BaseStructures.AspectStructure base = new AspectImporter(localPath).doImport();
                //Todo: Aspects that aren't necessarily on entities or having a "user uuid"
                return new Aspect(id, base, accessLevel);
            }, CompletionException::new));
            cancel(id); //Cancel any in progress one that already exists
            inProgressAspects.put(id, future);
        }

        /**
         * Call world render on all aspects
         */
        private void worldRender(VertexConsumerProvider vcp, float tickDelta, AspectMatrixStack matrices) {
            for (Aspect aspect : loadedAspects.values())
                aspect.renderWorld(vcp, tickDelta, matrices);
        }

    }








}
