package io.github.moonlightmaya.mixin.render.entity;

import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(EntityRenderDispatcher.class)
public interface EntityRenderDispatcherAccessor {
    @Accessor
    Map<EntityType<?>, EntityRenderer<?>> getRenderers();
    @Accessor
    Map<String, EntityRenderer<? extends PlayerEntity>> getModelRenderers();
}
