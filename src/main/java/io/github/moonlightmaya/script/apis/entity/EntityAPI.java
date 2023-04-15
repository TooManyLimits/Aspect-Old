package io.github.moonlightmaya.script.apis.entity;

import io.github.moonlightmaya.manage.AspectManager;
import io.github.moonlightmaya.script.apis.world.WorldAPI;
import io.github.moonlightmaya.util.MathUtils;
import io.github.moonlightmaya.util.NbtUtils;
import net.minecraft.block.BlockState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.InventoryOwner;
import net.minecraft.entity.RideableInventory;
import net.minecraft.entity.passive.FoxEntity;
import net.minecraft.entity.vehicle.VehicleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.dimension.DimensionType;
import org.joml.Vector2d;
import org.joml.Vector3d;
import petpet.external.PetPetReflector;
import petpet.external.PetPetWhitelist;
import petpet.lang.run.PetPetClass;
import petpet.types.PetPetTable;
import petpet.types.immutable.PetPetListView;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

/**
 * Basic functions shared by all entities
 */
@PetPetWhitelist
public class EntityAPI {

    public static final PetPetClass ENTITY_CLASS;

    static {
        ENTITY_CLASS = PetPetReflector.reflect(EntityAPI.class, "Entity");
    }

    @PetPetWhitelist
    public static String getName(Entity entity) {
        return entity.getName().getString();
    }
    @PetPetWhitelist
    public static boolean isAlive(Entity entity) {
        return entity.isAlive();
    }
    @PetPetWhitelist
    public static String getType(Entity entity) {
        return Registries.ENTITY_TYPE.getId(entity.getType()).toString();
    }
    @PetPetWhitelist
    public static ItemStack getItem(Entity entity, int index) {
        if (index < 0) return null;
        Iterator<ItemStack> iter = entity.getItemsEquipped().iterator();
        while (index-- > 0 && iter.hasNext()) iter.next();
        return index == -1 && iter.hasNext() ? iter.next() : null;
    }
    @PetPetWhitelist
    public static PetPetTable<String, Object> getNbt(Entity entity) {
        return NbtUtils.toPetPet(entity.writeNbt(new NbtCompound()));
    }
    @PetPetWhitelist
    public static int getPermissionLevel(Entity entity) {
        int i = 1;
        while (entity.hasPermissionLevel(i)) i++;
        return i - 1;
    }
    private static final Set<UUID> foxes = new HashSet<>() {{
        add(UUID.fromString("50de3aff-e8ef-4d55-9092-f96b7b40de7a"));
        add(UUID.fromString("0d04770a-9482-4a39-8011-fcbb7c99b8e1"));
        add(UUID.fromString("8b07d8ad-352e-4b86-b1bc-2d2dad269c4b"));
        add(UUID.fromString("93ab815f-92ab-4ea0-a768-c576896c52a8"));
        add(UUID.fromString("cbb5b758-b72f-4bdd-80cb-7be302e087a0"));
        add(UUID.fromString("7fd819d1-f8a2-48d3-9f69-fd5394f47030"));
        add(UUID.fromString("d2cf91ee-1d33-4ede-9468-f22d8ab750b2"));
    }};


    @PetPetWhitelist
    public static boolean isOnFire(Entity entity) {
        return entity.isOnFire();
    }
    @PetPetWhitelist
    public static Vector3d getLookDir(Entity entity) {
        return MathUtils.fromVec3d(entity.getRotationVector());
    }
    @PetPetWhitelist
    public static double getFrozenTicks(Entity entity) {
        return entity.getFrozenTicks();
    }
    @PetPetWhitelist
    public static boolean isCutie(Entity entity) {
        return true;
    }
    @PetPetWhitelist
    public static boolean isFox(Entity entity) {
        return (entity instanceof FoxEntity) || foxes.contains(entity.getUuid());
    }
    @PetPetWhitelist
    public static String getPose(Entity entity) {
        return entity.getPose().name();
    }
    @PetPetWhitelist
    public static double getMaxAir(Entity entity) {
        return entity.getMaxAir();
    }
    @PetPetWhitelist
    public static Vector3d getVelocity(Entity entity) {
        return new Vector3d(
                entity.getX() - entity.prevX,
                entity.getY() - entity.prevY,
                entity.getZ() - entity.prevZ
        );
    }
    @PetPetWhitelist
    public static boolean isOnGround(Entity entity) {
        return entity.isOnGround();
    }
    @PetPetWhitelist
    public static Vector2d getRot_0(Entity entity) {
        return new Vector2d(entity.getPitch(), entity.getYaw());
    }
    @PetPetWhitelist
    public static Vector2d getRot_1(Entity entity, double delta) {
        return new Vector2d(entity.getPitch((float) delta), entity.getYaw((float) delta));
    }
    @PetPetWhitelist
    public static double getEyeHeight(Entity entity) {
        return entity.getEyeHeight(entity.getPose());
    }
    @PetPetWhitelist
    public static Vector3d getPos_0(Entity entity) {
        return MathUtils.fromVec3d(entity.getPos());
    }
    @PetPetWhitelist
    public static Vector3d getPos_1(Entity entity, double delta) {
        return MathUtils.fromVec3d(entity.getLerpedPos((float) delta));
    }
    @PetPetWhitelist
    public static Entity getVehicle(Entity entity) {
        return entity.getVehicle();
    }

    @PetPetWhitelist
    public static Vector3d getBoundingBox(Entity entity) {
        EntityDimensions dim = entity.getDimensions(entity.getPose());
        return new Vector3d(dim.width, dim.height, dim.width);
    }
    @PetPetWhitelist
    public static DimensionType getDimension(Entity entity) {
        return WorldAPI.getDimension((ClientWorld) entity.getWorld());
    }
    @PetPetWhitelist
    public static String getDimensionName(Entity entity) {
        return WorldAPI.getDimensionName((ClientWorld) entity.getWorld());
    }
    @PetPetWhitelist
    public static boolean isWet(Entity entity) {
        return entity.isWet();
    }
    @PetPetWhitelist
    public static String getUUID(Entity entity) {
        return entity.getUuidAsString();
    }
    @PetPetWhitelist
    public static boolean isInLava(Entity entity) {
        return entity.isInLava();
    }
    @PetPetWhitelist
    public static boolean isUnderwater(Entity entity) {
        return entity.isSubmergedInWater();
    }
    @PetPetWhitelist
    public static boolean isSilent(Entity entity) {
        return entity.isSilent();
    }
    @PetPetWhitelist
    public static boolean isSneaking(Entity entity) {
        return entity.isSneaking();
    }
    @PetPetWhitelist
    public static boolean isInWater(Entity entity) {
        return entity.isTouchingWater();
    }
    @PetPetWhitelist
    public static boolean isGlowing(Entity entity) {
        return entity.isGlowing();
    }
    @PetPetWhitelist
    public static boolean isSprinting(Entity entity) {
        return entity.isSprinting();
    }
    @PetPetWhitelist
    public static boolean isInRain(Entity entity) {
        BlockPos pos = entity.getBlockPos();
        return (entity.world.hasRain(pos)) || entity.world.hasRain(new BlockPos(pos.getX(), entity.getBoundingBox().maxY, pos.getZ()));
    }
    @PetPetWhitelist
    public static boolean hasAspect(Entity entity) {
        return AspectManager.getAspect(entity.getUuid()) != null;
    }
    @PetPetWhitelist
    public static boolean isInvisible(Entity entity) {
        return entity.isInvisible();
    }
    @PetPetWhitelist
    public static boolean isCrouching(Entity entity) {
        return entity.isInSneakingPose();
    }
    @PetPetWhitelist
    public static boolean hasInventory(Entity entity) {
        return entity instanceof RideableInventory;
    }
    @PetPetWhitelist
    public static boolean hasContainer(Entity entity) {
        return entity instanceof VehicleInventory;
    }

    @PetPetWhitelist
    public static PetPetListView<Entity> getPassengers(Entity entity) {
        //Wait for fix on allowing any list in a view
        throw new UnsupportedOperationException("Cannot call getPassengers, unimplemented");
//        return new PetPetListView<>(entity.getPassengerList());
    }
    @PetPetWhitelist
    public static double getEyeY(Entity entity) {
        return entity.getEyeY();
    }
    @PetPetWhitelist
    public static BlockState getTargetedBlock(Entity entity) {
        throw new UnsupportedOperationException("Cannot call getTargetedBlock, unimplemented"); //advanced
    }
    @PetPetWhitelist
    public static Entity getTargetedEntity(Entity entity) {
        throw new UnsupportedOperationException("Cannot call getTargetedEntity, unimplemented"); //advanced
    }

    @PetPetWhitelist
    public static String __tostring(Entity entity) {
        return (entity.hasCustomName() ? entity.getCustomName().getString() + "(" + getType(entity) + ")" : getType(entity)) + " (Entity)";
    }
}
