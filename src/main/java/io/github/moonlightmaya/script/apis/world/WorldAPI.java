package io.github.moonlightmaya.script.apis.world;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.moonlightmaya.util.EntityUtils;
import io.github.moonlightmaya.util.MathUtils;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.BlockArgumentParser;
import net.minecraft.command.argument.ItemStackArgumentType;
import net.minecraft.command.argument.ParticleEffectArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LightType;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.dimension.DimensionType;
import org.joml.Vector3d;
import petpet.external.PetPetReflector;
import petpet.external.PetPetWhitelist;
import petpet.lang.run.*;
import petpet.types.PetPetList;

import java.util.HashMap;
import java.util.UUID;
import java.util.function.Function;

@PetPetWhitelist
public class WorldAPI {

    public static final PetPetClass WORLD_CLASS;

    static {
        //Custom methods, inside the WorldAPI class
        WORLD_CLASS = PetPetReflector.reflect(WorldAPI.class, "World");

        ((JavaFunction) WORLD_CLASS.methods.get("eachBlock_3")).costPenalizer = i -> {
            Vector3d max = (Vector3d) i.peek(1);
            Vector3d min = (Vector3d) i.peek(2);
            return (int) (
                    Math.abs(Math.floor(max.z) - Math.floor(min.z)) *
                    Math.abs(Math.floor(max.y) - Math.floor(min.y)) *
                    Math.abs(Math.floor(max.x) - Math.floor(min.x))
            );
        };
        ((JavaFunction) WORLD_CLASS.methods.get("eachBlock_7")).costPenalizer = i -> {
            double maxZ = Math.floor((Double) i.peek(1));
            double maxY = Math.floor((Double) i.peek(2));
            double maxX = Math.floor((Double) i.peek(3));
            double minZ = Math.floor((Double) i.peek(4));
            double minY = Math.floor((Double) i.peek(5));
            double minX = Math.floor((Double) i.peek(6));
            return (int) (Math.abs(maxZ - minZ) * Math.abs(maxY - minY) * Math.abs(maxX - minX));
        };
    }

    private static <T> T acceptPosOrElse(ClientWorld world, double x, double y, double z, Function<BlockPos, T> ifLoaded, T ifUnloaded) {
        BlockPos blockPos = new BlockPos((int) x, (int) y, (int) z);
        if (world.getChunk(blockPos) == null)
            return ifUnloaded;
        return ifLoaded.apply(blockPos);
    }

    @PetPetWhitelist
    public static double getTime_0(ClientWorld world) {
        return world.getTime();
    }
    @PetPetWhitelist
    public static double getTime_1(ClientWorld world, double delta) {
        return world.getTime() + delta;
    }
    @PetPetWhitelist
    public static double getTime_2(ClientWorld world, double delta, double speed) {
        return speed * (world.getTime() + delta);
    }
    @PetPetWhitelist
    public static double getTimeOfDay_0(ClientWorld world) {
        return world.getTimeOfDay();
    }
    @PetPetWhitelist
    public static double getTimeOfDay_1(ClientWorld world, double delta) {
        return world.getTimeOfDay() + delta;
    }
    @PetPetWhitelist
    public static DimensionType getDimension(ClientWorld world) {
        return world.getDimension();
    }
    @PetPetWhitelist
    public static String getDimensionName(ClientWorld world) {
        return world.getDimensionKey().getValue().toString();
    }
    @PetPetWhitelist
    public static double getMoonPhase(ClientWorld world) {
        return world.getMoonPhase();
    }

    @PetPetWhitelist
    public static PetPetList<Entity> getEntities(ClientWorld world) {
        PetPetList<Entity> entities = new PetPetList<>();
        world.getEntities().forEach(entities::add);
        return entities;
    }
    @PetPetWhitelist
    public static Entity getEntity(ClientWorld world, String uuid) {
        return EntityUtils.getEntityByUUID(world, UUID.fromString(uuid));
    }
    @PetPetWhitelist
    public static double getStrongRedstone_1(ClientWorld world, Vector3d pos) {
        return acceptPosOrElse(world, pos.x, pos.y, pos.z, world::getReceivedStrongRedstonePower, 0);
    }
    @PetPetWhitelist
    public static double getStrongRedstone_3(ClientWorld world, double x, double y, double z) {
        return acceptPosOrElse(world, x, y, z, world::getReceivedStrongRedstonePower, 0);
    }
    @PetPetWhitelist
    public static double getRedstone_1(ClientWorld world, Vector3d pos) {
        return acceptPosOrElse(world, pos.x, pos.y, pos.z, world::getReceivedRedstonePower, 0);
    }
    @PetPetWhitelist
    public static double getRedstone_3(ClientWorld world, double x, double y, double z) {
        return acceptPosOrElse(world, x, y, z, world::getReceivedRedstonePower, 0);
    }
    @PetPetWhitelist
    public static Biome getBiome_1(ClientWorld world, Vector3d pos) {
        return world.getBiome(new BlockPos((int) pos.x, (int) pos.y, (int) pos.z)).value();
    }
    @PetPetWhitelist
    public static Biome getBiome_3(ClientWorld world, double x, double y, double z) {
        return world.getBiome(new BlockPos((int) x, (int) y, (int) z)).value();
    }
    @PetPetWhitelist
    public static String getBiomeID_1(ClientWorld world, Vector3d pos) {
        return world.getBiome(new BlockPos((int) pos.x, (int) pos.y, (int) pos.z)).getKey().map(k -> k.getValue().toString()).orElse(null);
    }
    @PetPetWhitelist
    public static String getBiomeID_3(ClientWorld world, double x, double y, double z) {
        return world.getBiome(new BlockPos((int) x, (int) y, (int) z)).getKey().map(k -> k.getValue().toString()).orElse(null);
    }
    @PetPetWhitelist
    public static BlockState getBlockState_1(ClientWorld world, Vector3d pos) {
        return acceptPosOrElse(world, pos.x, pos.y, pos.z, world::getBlockState, Blocks.AIR.getDefaultState());
    }
    @PetPetWhitelist
    public static BlockState getBlockState_3(ClientWorld world, double x, double y, double z) {
        return acceptPosOrElse(world, x, y, z, world::getBlockState, Blocks.AIR.getDefaultState());
    }
    @PetPetWhitelist
    public static boolean eachBlock_3(ClientWorld world, Vector3d min, Vector3d max, PetPetCallable func) {
        return eachBlock_7(world, min.x, min.y, min.z, max.x, max.y, max.z, func);
    }
    @PetPetWhitelist
    public static boolean eachBlock_7(ClientWorld world, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, PetPetCallable func) {
        if (func.paramCount() != 4)
            throw new PetPetException("world.eachBlock() expects a function with 4 params, but you passed in one with " + func.paramCount() + " params");
        BlockPos a = MathUtils.getBlockPos(minX, minY, minZ);
        BlockPos b = MathUtils.getBlockPos(maxX, maxY, maxZ);
        //If any part of the region is unloaded, then do nothing and return false
        if (!world.isRegionLoaded(a, b))
            return false;
        //Otherwise, call the function on each block in range
        BlockPos.stream(a, b).forEachOrdered(blockPos -> func.call(world.getBlockState(blockPos), blockPos.getX(), blockPos.getY(), blockPos.getZ()));
        //Indicate success by returning true
        return true;
    }
    @PetPetWhitelist
    public static Integer getLight_1(ClientWorld world, Vector3d pos) {
        return acceptPosOrElse(world, pos.x, pos.y, pos.z, world::getLightLevel, null);
    }
    @PetPetWhitelist
    public static Integer getLight_3(ClientWorld world, double x, double y, double z) {
        return acceptPosOrElse(world, x, y, z, world::getLightLevel, null);
    }
    @PetPetWhitelist
    public static Integer getBlockLight_1(ClientWorld world, Vector3d pos) {
        return acceptPosOrElse(world, pos.x, pos.y, pos.z, p -> world.getLightLevel(LightType.BLOCK, p), null);
    }
    @PetPetWhitelist
    public static Integer getBlockLight_3(ClientWorld world, double x, double y, double z) {
        return acceptPosOrElse(world, x, y, z, p -> world.getLightLevel(LightType.BLOCK, p), null);
    }
    @PetPetWhitelist
    public static Integer getSkyLight_1(ClientWorld world, Vector3d pos) {
        return acceptPosOrElse(world, pos.x, pos.y, pos.z, p -> world.getLightLevel(LightType.SKY, p), null);
    }
    @PetPetWhitelist
    public static Integer getSkyLight_3(ClientWorld world, double x, double y, double z) {
        return acceptPosOrElse(world, x, y, z, p -> world.getLightLevel(LightType.SKY, p), null);
    }
    @PetPetWhitelist
    public static double getRainGradient_0(ClientWorld world) {
        return world.getRainGradient(1f);
    }
    @PetPetWhitelist
    public static double getRainGradient_1(ClientWorld world, double delta) {
        return world.getRainGradient((float) delta);
    }
    @PetPetWhitelist
    public static boolean isThundering(ClientWorld world) {
        return world.isThundering();
    }
    @PetPetWhitelist
    public static Boolean isOpenSky_1(ClientWorld world, Vector3d pos) {
        return acceptPosOrElse(world, pos.x, pos.y, pos.z, world::isSkyVisible, null);
    }
    @PetPetWhitelist
    public static Boolean isOpenSky_3(ClientWorld world, double x, double y, double z) {
        return acceptPosOrElse(world, x, y, z, world::isSkyVisible, null);
    }

    @PetPetWhitelist
    public static BlockState newBlock(ClientWorld world, String str) {
        try {
            return BlockArgumentParser.block(world.createCommandRegistryWrapper(Registries.BLOCK.getKey()), str, true).blockState();
        } catch (CommandSyntaxException ex) {
            throw new PetPetException("Failed to parse BlockState from string: " + str);
        }
    }

    @PetPetWhitelist
    public static ItemStack newItem(ClientWorld world, String str) {
        try {
            return ItemStackArgumentType
                    .itemStack(CommandRegistryAccess.of(world.getRegistryManager(), world.getEnabledFeatures()))
                    .parse(new StringReader(str))
                    .createStack(1, false);
        } catch (CommandSyntaxException ex) {
            throw new PetPetException("Failed to parse ItemStack from string: " + str);
        }
    }

    @PetPetWhitelist
    public static String __tostring(ClientWorld world) {
        return "World";
    }
}
