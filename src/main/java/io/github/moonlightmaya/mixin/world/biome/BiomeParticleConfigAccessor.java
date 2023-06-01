package io.github.moonlightmaya.mixin.world.biome;

import net.minecraft.world.biome.BiomeParticleConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BiomeParticleConfig.class)
public interface BiomeParticleConfigAccessor {
    @Accessor
    float getProbability();
}
