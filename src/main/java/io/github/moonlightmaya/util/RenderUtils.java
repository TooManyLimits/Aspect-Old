package io.github.moonlightmaya.util;

import com.mojang.blaze3d.systems.RenderCall;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.moonlightmaya.model.renderlayers.NewRenderLayerFunction;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.entity.Entity;
import org.jetbrains.annotations.Nullable;

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
}
