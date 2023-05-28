package io.github.moonlightmaya.mixin.render.particle;

import io.github.moonlightmaya.quack.BillboardParticleAccessor;
import net.minecraft.client.particle.BillboardParticle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BillboardParticle.class)
public class BillboardParticleMixin implements BillboardParticleAccessor {

    @Shadow
    protected float scale;

    /**
     * The billboard particle constructor provides a random scale,
     * which is not ideal when the user wants to precisely control
     * the scale of particles through script. So this function will
     * force a particle to be set to its maximum possible randomly
     * assigned size, making the particle scale consistent if you
     * use the scale-changing functions.
     */
    @Override
    public void aspect$setDefiniteSize() {
        this.scale = 0.2f;
    }
}
