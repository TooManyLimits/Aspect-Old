package io.github.moonlightmaya.util;

import com.mojang.blaze3d.systems.RenderCall;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.moonlightmaya.model.renderlayers.NewRenderLayerFunction;
import io.github.moonlightmaya.util.compat.SodiumCompat;
import me.jellysquid.mods.sodium.client.render.vertex.VertexBufferWriter;
import me.jellysquid.mods.sodium.client.render.vertex.VertexFormatDescription;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.entity.Entity;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4d;
import org.lwjgl.system.MemoryStack;

public class RenderUtils {

    /**
     * Says "RenderCall", but is just a runnable
     */
    public static void executeOnRenderThread(RenderCall r) {
        if (RenderSystem.isOnRenderThreadOrInit())
            r.execute();
        else
            RenderSystem.recordRenderCall(r);
    }

    public static <T extends Entity> EntityRenderer<? super T> getRenderer(T entity) {
        return MinecraftClient.getInstance().getEntityRenderDispatcher().getRenderer(entity);
    }

    public static @Nullable EntityModel<?> getModel(Entity entity) {
        EntityRenderer<?> renderer = MinecraftClient.getInstance().getEntityRenderDispatcher().getRenderer(entity);
        if (renderer instanceof LivingEntityRenderer<?,?> living) {
            return living.getModel();
        }
        return null;
    }

    public static RenderLayer getDirectEntityFlint() {
        return NewRenderLayerFunction.flint;
    }

    /**
     * Keep track of the world <=> view matrices, update each frame
     */
    public static final Matrix4d WORLD_TO_VIEW_MATRIX = new Matrix4d();
    public static final Matrix4d VIEW_TO_WORLD_MATRIX = new Matrix4d();
    public static void updateWorldViewMatrices(Matrix4d worldToView) {
        WORLD_TO_VIEW_MATRIX.set(worldToView);
        WORLD_TO_VIEW_MATRIX.invert(VIEW_TO_WORLD_MATRIX);
    }

    /**
     * A silly little VCP which just does nothing with any vertices it's given.
     * This is helpful inside mixins when we want to fool some rendering methods
     * into thinking they're doing something, when really they're just sending
     * vertices into the void.
     */

    private static VertexConsumerProvider sillyLittleVcp = null;
    private static VertexConsumer sillyLittleVc = null;

    public static VertexConsumerProvider getSillyLittleVcp() {
        if (sillyLittleVcp == null) {
            //Sodium doesn't like this, so we need a special workaround to return its own instance
            if (FabricLoader.getInstance().isModLoaded("sodium")) {
                sillyLittleVc = SodiumCompat.SillyLittleSodiumVertexConsumer.instance;
            } else {
                sillyLittleVc = new VertexConsumer() {
                    @Override
                    public VertexConsumer vertex(double x, double y, double z) {return this;}
                    @Override
                    public VertexConsumer color(int red, int green, int blue, int alpha) {return this;}
                    @Override
                    public VertexConsumer texture(float u, float v) {return this;}
                    @Override
                    public VertexConsumer overlay(int u, int v) {return this;}
                    @Override
                    public VertexConsumer light(int u, int v) {return this;}
                    @Override
                    public VertexConsumer normal(float x, float y, float z) {return this;}
                    @Override
                    public void next() {}
                    @Override
                    public void fixedColor(int red, int green, int blue, int alpha) {}
                    @Override
                    public void unfixColor() {}
                };
            }
            //Vcp just provides the do-nothing vertex consumer
            sillyLittleVcp = layer -> sillyLittleVc;
        }
        return sillyLittleVcp;
    }

}
