package io.github.moonlightmaya.script.apis.world;

import io.github.moonlightmaya.mixin.world.biome.BiomeAccessor;
import io.github.moonlightmaya.mixin.world.biome.BiomeParticleConfigAccessor;
import io.github.moonlightmaya.util.ColorUtils;
import io.github.moonlightmaya.util.MathUtils;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeEffects;
import net.minecraft.world.biome.BiomeParticleConfig;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2d;
import org.joml.Vector3d;
import org.joml.Vector4d;
import petpet.external.PetPetWhitelist;

@PetPetWhitelist
public class BiomeAPI {
    @PetPetWhitelist
    public static boolean hasPrecipitation(Biome biome) {
        return biome.hasPrecipitation();
    }
    @PetPetWhitelist
    public static Vector4d skyColor(Biome biome) {
        return ColorUtils.intARGBToVec(biome.getSkyColor());
    }
    @PetPetWhitelist
    public static Vector4d fogColor(Biome biome) {
        return ColorUtils.intARGBToVec(biome.getFogColor());
    }
    @PetPetWhitelist
    public static double temperature_0(Biome biome) {
        return biome.getTemperature();
    }
    @PetPetWhitelist
    public static double temperature_1(Biome biome, Vector3d pos) {
        return ((BiomeAccessor) (Object) biome).invokeGetTemperature(MathUtils.getBlockPos(pos));
    }
    @PetPetWhitelist
    public static double temperature_3(Biome biome, double x, double y, double z) {
        return ((BiomeAccessor) (Object) biome).invokeGetTemperature(MathUtils.getBlockPos(x, y, z));
    }
    @PetPetWhitelist
    public static boolean isCold_0(Biome biome) {
        return biome.isCold(BlockPos.ORIGIN);
    }
    @PetPetWhitelist
    public static boolean isCold_1(Biome biome, Vector3d pos) {
        return biome.isCold(MathUtils.getBlockPos(pos));
    }
    @PetPetWhitelist
    public static boolean isCold_3(Biome biome, double x, double y, double z) {
        return biome.isCold(MathUtils.getBlockPos(x, y, z));
    }

    /**
     * Gets the grass color for the biome at 0,0.
     * Usually good enough, except for swamps, which have
     * extra handling depending on the x,z coords.
     */
    @PetPetWhitelist
    public static Vector4d grassColor_0(Biome biome) {
        return ColorUtils.intARGBToVec(biome.getGrassColorAt(0, 0));
    }
    @PetPetWhitelist
    public static Vector4d grassColor_1(Biome biome, Vector2d xz) {
        int x = MathHelper.floor(xz.x);
        int z = MathHelper.floor(xz.y);
        return ColorUtils.intARGBToVec(biome.getGrassColorAt(x, z));
    }
    @PetPetWhitelist
    public static Vector4d grassColor_2(Biome biome, double x, double z) {
        int x2 = MathHelper.floor(x);
        int z2 = MathHelper.floor(z);
        return ColorUtils.intARGBToVec(biome.getGrassColorAt(x2, z2));
    }
    @PetPetWhitelist
    public static Vector4d foliageColor(Biome biome) {
        return ColorUtils.intARGBToVec(biome.getFoliageColor());
    }
    @PetPetWhitelist
    public static Vector4d waterColor(Biome biome) {
        return ColorUtils.intARGBToVec(biome.getWaterColor());
    }
    @PetPetWhitelist
    public static Vector4d waterFogColor(Biome biome) {
        return ColorUtils.intARGBToVec(biome.getWaterFogColor());
    }
    @PetPetWhitelist @Nullable
    public static ParticleEffect particleType(Biome biome) {
        return biome.getParticleConfig().map(BiomeParticleConfig::getParticle).orElse(null);
    }

    /**
     * Why would anyone ever use this lmao
     * why did i spend an entire 5 minutes
     * making the mixin for this method
     */
    @PetPetWhitelist @Nullable
    public static Double particleChance(Biome biome) {
        return biome.getParticleConfig()
                .map(config -> (double) ((BiomeParticleConfigAccessor) config).getProbability())
                .orElse(null);
    }
    @PetPetWhitelist
    public static String __tostring(Biome biome) {
        return "Biome";
    }
}
