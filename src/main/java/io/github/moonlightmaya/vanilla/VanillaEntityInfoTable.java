package io.github.moonlightmaya.vanilla;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.render.entity.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.util.IdentityHashMap;

/**
 * Keep track of the scales of certain vanilla entities,
 * avoiding some matrix calculations in favor of a lookup
 */
public class VanillaEntityInfoTable {

    /**
     * The scale of various entities as applied in the LivingEntityRenderer.scale() function.
     * If the entity has a specially calculated scale, it is not entered in this map and is instead
     * computed each time. For instance, the Creeper's scale changes dynamically as its fuse ticks.
     */
    public static final IdentityHashMap<Class<?>, Float> SCALES = new IdentityHashMap<>();

    public static @Nullable Float getScale(Class<?> entityRendererClass) {
        return SCALES.get(entityRendererClass);
    }

    static {
        //Scales collected by looking at overrides of LivingEntityRenderer.scale()

        //Random mobs
        SCALES.put(BatEntityRenderer.class, 0.35f);
        SCALES.put(CatEntityRenderer.class, 0.8f);
        SCALES.put(CaveSpiderEntityRenderer.class, 0.7f);
        SCALES.put(ElderGuardianEntityRenderer.class, ElderGuardianEntity.SCALE);
        SCALES.put(GhastEntityRenderer.class, 4.5f);
        SCALES.put(GiantEntityRenderer.class, 6f);
        SCALES.put(HuskEntityRenderer.class, 1.0625f);
        SCALES.put(PolarBearEntityRenderer.class, 1.2f);
        SCALES.put(WitherSkeletonEntityRenderer.class, 1.2f);

        //Player
        SCALES.put(PlayerEntityRenderer.class, 0.9375f);

        //Illagers
        SCALES.put(EvokerEntityRenderer.class, 0.9375f);
        SCALES.put(IllusionerEntityRenderer.class, 0.9375f);
        SCALES.put(PillagerEntityRenderer.class, 0.9375f);
        SCALES.put(VindicatorEntityRenderer.class, 0.9375f);
        SCALES.put(WitchEntityRenderer.class, 0.9375f);
        SCALES.put(WanderingTraderEntityRenderer.class, 0.9375f);
    }
}
