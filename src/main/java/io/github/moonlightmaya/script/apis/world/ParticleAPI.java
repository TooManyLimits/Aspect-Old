package io.github.moonlightmaya.script.apis.world;

import io.github.moonlightmaya.mixin.render.particle.ParticleAccessor;
import io.github.moonlightmaya.quack.BillboardParticleAccessor;
import net.minecraft.client.particle.BillboardParticle;
import net.minecraft.client.particle.FishingParticle;
import net.minecraft.client.particle.Particle;
import org.joml.Vector3d;
import org.joml.Vector4d;
import petpet.external.PetPetReflector;
import petpet.external.PetPetWhitelist;
import petpet.lang.run.PetPetClass;

/**
 * Methods to call on vanilla Particle objects
 */
@PetPetWhitelist
public class ParticleAPI {

    @PetPetWhitelist
    public static Particle pos_1(Particle particle, Vector3d pos) {
        particle.setPos(pos.x, pos.y, pos.z);
        return particle;
    }

    @PetPetWhitelist
    public static Particle pos_3(Particle particle, double x, double y, double z) {
        particle.setPos(x, y, z);
        return particle;
    }

    @PetPetWhitelist
    public static Particle vel_1(Particle particle, Vector3d vel) {
        particle.setVelocity(vel.x, vel.y, vel.z);
        return particle;
    }

    @PetPetWhitelist
    public static Particle vel_3(Particle particle, double x, double y, double z) {
        particle.setVelocity(x, y, z);
        return particle;
    }

    @PetPetWhitelist
    public static Particle scale_1(Particle particle, double scale) {
        if (particle instanceof BillboardParticle billboard)
            ((BillboardParticleAccessor) billboard).aspect$setDefiniteSize();
        particle.scale((float) scale);
        return particle;
    }

    @PetPetWhitelist
    public static Particle color_1(Particle particle, Vector4d color) {
        particle.setColor((float) color.x, (float) color.y, (float) color.z);
        ((ParticleAccessor) particle).setAlpha((float) color.w);
        return particle;
    }

    @PetPetWhitelist
    public static Particle color_4(Particle particle, double r, double g, double b, double a) {
        particle.setColor((float) r, (float) g, (float) b);
        ((ParticleAccessor) particle).setAlpha((float) a);
        return particle;
    }

    /**
     * Accelerate the particle in the direction it's already moving
     */
    @PetPetWhitelist
    public static Particle power_1(Particle particle, double power) {
        particle.move((float) power);
        return particle;
    }

    /**
     * Set lifetime of the particle, in ticks
     */
    @PetPetWhitelist
    public static Particle lifetime_1(Particle particle, double lifetime) {
        if (lifetime < 0)
            lifetime = 0;
        //I don't know why figura has this clause for fishing particles, but
        //I'm just going to add it also because there was probably a reason
        else if (particle instanceof FishingParticle && lifetime > 60)
            lifetime = 60;
        particle.setMaxAge((int) lifetime);
        return particle;
    }

    @PetPetWhitelist
    public static Particle gravity_1(Particle particle, double gravity) {
        ((ParticleAccessor) particle).setGravityStrength((float) gravity);
        return particle;
    }

    @PetPetWhitelist
    public static Particle collision_1(Particle particle, boolean collision) {
        ((ParticleAccessor) particle).setCollidesWithWorld(collision);
        return particle;
    }

    @PetPetWhitelist
    public static void remove_0(Particle particle) {
        particle.markDead();
    }

    @PetPetWhitelist
    public static String __tostring(Particle particle) {
        return particle.toString();
    }

}
