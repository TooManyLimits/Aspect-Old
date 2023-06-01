package io.github.moonlightmaya.script.apis.world;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.DimensionEffects;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector4d;
import petpet.external.PetPetWhitelist;

@PetPetWhitelist
public class DimensionEffectsAPI {

    /**
     * Returns the fog color override of this dimension
     * If it doesn't have one, returns a vec of all 1
     */
    @PetPetWhitelist
    public static Vector4d getFogColorOverride_2(DimensionEffects effects, float skyAngle, float tickDelta) {
        float[] override = effects.getFogColorOverride(skyAngle, tickDelta);
        return new Vector4d(
                override == null ? 1 : override[0],
                override == null ? 1 : override[1],
                override == null ? 1 : override[2],
                override == null ? 1 : override[3]
        );
    }

    /**
     * Returns the fog color override of this dimension given the sky angle.
     * Gets the tick delta from Minecraft.
     */
    @PetPetWhitelist
    public static Vector4d getFogColorOverride_1(DimensionEffects effects, float skyAngle) {
        return getFogColorOverride_2(effects, skyAngle, MinecraftClient.getInstance().getTickDelta());
    }

    /**
     * Returns the fog color override of this dimension. Takes the sky angle
     * from the currently loaded Minecraft world, and the tick delta from
     * Minecraft as well.
     * If there is no loaded world, then assumes the sky angle is 0.
     */
    @PetPetWhitelist
    public static Vector4d getFogColorOverride_0(DimensionEffects effects) {
        ClientWorld world = MinecraftClient.getInstance().world;
        float tickDelta = MinecraftClient.getInstance().getTickDelta();
        float skyAngle = world != null ? world.getSkyAngle(tickDelta) : 0;
        return getFogColorOverride_2(effects, skyAngle, tickDelta);
    }

    @PetPetWhitelist
    public static double getCloudsHeight(DimensionEffects effects) {
        return effects.getCloudsHeight();
    }

    @PetPetWhitelist
    public static boolean hasAlternateSkyColor(DimensionEffects effects) {
        return effects.isAlternateSkyColor();
    }

    /**
     * NONE, NORMAL, or END
     */
    @PetPetWhitelist
    public static String getSkyType(DimensionEffects effects) {
        return effects.getSkyType().name();
    }

    /**
     * In vanilla this returns true for the nether and false for other
     * dimensions. The parameters are the X and Y of your camera, for
     * some reason, though these are never used for anything in vanilla.
     */
    @PetPetWhitelist
    public static boolean useThickFog_2(DimensionEffects effects, double x, double y) {
        return effects.useThickFog(MathHelper.floor(x), MathHelper.floor(y));
    }

    /**
     * In vanilla this returns true for the nether and false for other
     * dimensions. Assumes the X and Y are the X and Y coords of the
     * client's camera.
     */
    @PetPetWhitelist
    public static boolean useThickFog_0(DimensionEffects effects) {
        Vec3d pos = MinecraftClient.getInstance().gameRenderer.getCamera().getPos();
        return effects.useThickFog(MathHelper.floor(pos.x), MathHelper.floor(pos.y));
    }

    @PetPetWhitelist
    public static boolean shouldBrightenLighting(DimensionEffects effects) {
        return effects.shouldBrightenLighting();
    }

    @PetPetWhitelist
    public static boolean isDarkened(DimensionEffects effects) {
        return effects.isDarkened();
    }


    @PetPetWhitelist
    public static String __tostring(DimensionEffects effects) {
        return "DimensionEffects";
    }

}
