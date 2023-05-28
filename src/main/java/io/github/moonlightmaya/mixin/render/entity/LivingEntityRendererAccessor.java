package io.github.moonlightmaya.mixin.render.entity;

import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(LivingEntityRenderer.class)
public interface LivingEntityRendererAccessor {
    @Invoker("getAnimationCounter")
    float animationCounter(LivingEntity e, float tickDelta);
    @Accessor
    List<FeatureRenderer<?, ?>> getFeatures();
}
