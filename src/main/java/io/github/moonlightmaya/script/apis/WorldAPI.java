package io.github.moonlightmaya.script.apis;

import io.github.moonlightmaya.mixin.world.ClientWorldInvoker;
import io.github.moonlightmaya.util.MathUtils;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.LightType;
import net.minecraft.world.LunarWorldView;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import petpet.external.PetPetReflector;
import petpet.external.PetPetWhitelist;
import petpet.lang.run.*;
import petpet.types.PetPetList;
import petpet.types.immutable.PetPetListView;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.ToIntFunction;

public class WorldAPI {

    public static final PetPetClass WORLD_CLASS;

    static {
        //Custom methods, inside the WorldAPI class
        WORLD_CLASS = PetPetReflector.reflect(WorldAPI.class, "World");

        //Vanilla methods, yoink
        WORLD_CLASS.addMethod("getTime_0", new JavaFunction(World.class, "getTime", true));
        WORLD_CLASS.addMethod("getTimeOfDay_0", new JavaFunction(ClientWorld.class, "getTimeOfDay", true));
        WORLD_CLASS.addMethod("getDimension", new JavaFunction(World.class, "getDimension", true)); //Need to register DimensionType class
        WORLD_CLASS.addMethod("getMoonPhase", new JavaFunction(LunarWorldView.class, "getMoonPhase", true));

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
        BlockPos blockPos = new BlockPos(x, y, z);
        if (world.getChunk(blockPos) == null)
            return ifUnloaded;
        return ifLoaded.apply(blockPos);
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
    public static double getTimeOfDay_1(ClientWorld world, double delta) {
        return world.getTime() + delta;
    }
    @PetPetWhitelist
    public static PetPetList<Entity> getEntities(ClientWorld world) {
        PetPetList<Entity> entities = new PetPetList<>();
        world.getEntities().forEach(entities::add);
        return entities;
    }
    @PetPetWhitelist
    public static Entity getEntity(ClientWorld world, UUID uuid) {
        return ((ClientWorldInvoker) world).getEntityLookup().get(uuid);
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
        return world.getBiome(new BlockPos(pos.x, pos.y, pos.z)).value();
    }
    @PetPetWhitelist
    public static Biome getBiome_3(ClientWorld world, double x, double y, double z) {
        return world.getBiome(new BlockPos(x, y, z)).value();
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
    public static void eachBlock_3(ClientWorld world, Vector3d min, Vector3d max, PetPetCallable func) {
        eachBlock_7(world, min.x, min.y, min.z, max.x, max.y, max.z, func);
    }
    @PetPetWhitelist
    public static void eachBlock_7(ClientWorld world, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, PetPetCallable func) {
        if (func.paramCount() != 4)
            throw new PetPetException("world.eachBlock() expects a function with 4 params, but you passed in one with " + func.paramCount() + " params");
        BlockPos a = new BlockPos(minX, minY, minZ);
        BlockPos b = new BlockPos(maxX, maxY, maxZ);
        //If any part of the region is unloaded, then do nothing
        if (!world.isRegionLoaded(a, b))
            return;
        //Otherwise, call the function on each block in range
        BlockPos.stream(a, b).forEachOrdered(blockPos -> func.call(world.getBlockState(blockPos), blockPos.getX(), blockPos.getY(), blockPos.getZ()));
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
}
