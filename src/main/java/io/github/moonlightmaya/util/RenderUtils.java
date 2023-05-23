package io.github.moonlightmaya.util;

import com.mojang.blaze3d.systems.RenderCall;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.moonlightmaya.model.renderlayers.NewRenderLayerFunction;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.entity.Entity;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4d;

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
}
