package io.github.moonlightmaya.mixin.render.particle;

import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Particle.class)
public interface ParticleAccessor {
    @Accessor
    void setAlpha(float alpha);
    @Accessor
    void setGravityStrength(float strength);
    @Accessor
    void setCollidesWithWorld(boolean collides);
}
