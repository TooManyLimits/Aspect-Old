package io.github.moonlightmaya.script.apis.entity;

import io.github.moonlightmaya.Aspect;
import io.github.moonlightmaya.manage.AspectManager;
import io.github.moonlightmaya.script.apis.AspectAPI;
import io.github.moonlightmaya.script.apis.world.WorldAPI;
import io.github.moonlightmaya.util.GroupUtils;
import io.github.moonlightmaya.util.MathUtils;
import io.github.moonlightmaya.util.NbtUtils;
import net.minecraft.block.BlockState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.RideableInventory;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.vehicle.VehicleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.dimension.DimensionType;
import org.joml.Vector2d;
import org.joml.Vector3d;
import petpet.external.PetPetWhitelist;
import petpet.types.PetPetList;
import petpet.types.PetPetTable;
import petpet.types.immutable.PetPetListView;

import java.util.Iterator;

/**
 * Basic functions shared by all entities
 */
@PetPetWhitelist
public class EntityAPI {

    /**
     * boolean
     */
    @PetPetWhitelist
    public static boolean isAlive(Entity entity) {
        return entity.isAlive();
    }
    @PetPetWhitelist
    public static boolean isOnFire(Entity entity) {
        return entity.isOnFire();
    }
    @PetPetWhitelist
    public static boolean isOnGround(Entity entity) {
        return entity.isOnGround();
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
    public static boolean isGlowing(Entity entity) {
        return entity.isGlowing();
    }
    @PetPetWhitelist
    public static boolean isSprinting(Entity entity) {
        return entity.isSprinting();
    }
    @PetPetWhitelist
    public static boolean hasAspect(Entity entity) {
        return AspectManager.getAspect(entity) != null;
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

    /**
     * fluid
     */
    @PetPetWhitelist
    public static boolean isWet(Entity entity) {
        return entity.isWet();
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
    public static boolean isInWater(Entity entity) {
        return entity.isTouchingWater();
    }
    @PetPetWhitelist
    public static boolean isInRain(Entity entity) {
        BlockPos pos = entity.getBlockPos();
        return (entity.world.hasRain(pos)) || entity.world.hasRain(MathUtils.getBlockPos(pos.getX(), (int) entity.getBoundingBox().maxY, pos.getZ()));
    }

    /**
     * number
     */
    @PetPetWhitelist
    public static int permissionLevel(Entity entity) {
        int i = 1;
        while (entity.hasPermissionLevel(i)) i++;
        return i - 1;
    }
    @PetPetWhitelist
    public static double frozenTicks(Entity entity) {
        return entity.getFrozenTicks();
    }
    @PetPetWhitelist
    public static double maxAir(Entity entity) {
        return entity.getMaxAir();
    }
    @PetPetWhitelist
    public static double eyeHeight(Entity entity) {
        return entity.getEyeHeight(entity.getPose());
    }
    @PetPetWhitelist
    public static double eyeY(Entity entity) {
        return entity.getEyeY();
    }

    /**
     * vector
     */
    @PetPetWhitelist
    public static Vector3d pos_0(Entity entity) {
        return MathUtils.fromVec3d(entity.getPos());
    }
    @PetPetWhitelist
    public static Vector3d pos_1(Entity entity, double delta) {
        return MathUtils.fromVec3d(entity.getLerpedPos((float) delta));
    }
    @PetPetWhitelist
    public static Vector2d rot_0(Entity entity) {
        return new Vector2d(entity.getPitch(), entity.getYaw());
    }
    @PetPetWhitelist
    public static Vector2d rot_1(Entity entity, double delta) {
        return new Vector2d(entity.getPitch((float) delta), entity.getYaw((float) delta));
    }
    @PetPetWhitelist
    public static Vector3d vel(Entity entity) {
        return new Vector3d(
                entity.getX() - entity.prevX,
                entity.getY() - entity.prevY,
                entity.getZ() - entity.prevZ
        );
    }
    @PetPetWhitelist
    public static Vector3d lookDir(Entity entity) {
        return MathUtils.fromVec3d(entity.getRotationVector());
    }
    @PetPetWhitelist
    public static Vector3d boundingBox(Entity entity) {
        EntityDimensions dim = entity.getDimensions(entity.getPose());
        return new Vector3d(dim.width, dim.height, dim.width);
    }


    /**
     * string
     */

    @PetPetWhitelist
    public static String name(Entity entity) {
        return entity.getName().getString();
    }
    @PetPetWhitelist
    public static String type(Entity entity) {
        return Registries.ENTITY_TYPE.getId(entity.getType()).toString();
    }
    @PetPetWhitelist
    public static String pose(Entity entity) {
        return entity.getPose().name();
    }
    @PetPetWhitelist
    public static String uuid(Entity entity) {
        return entity.getUuidAsString();
    }
    @PetPetWhitelist
    public static String dimensionName(Entity entity) {
        return WorldAPI.dimensionName((ClientWorld) entity.getWorld());
    }

    /**
     * other
     */
    @PetPetWhitelist
    public static ItemStack item(Entity entity, int index) {
        if (index < 0) return null;
        Iterator<ItemStack> iter = entity.getItemsEquipped().iterator();
        while (index-- > 0 && iter.hasNext()) iter.next();
        return index == -1 && iter.hasNext() ? iter.next() : null;
    }
    @PetPetWhitelist
    public static PetPetTable<String, Object> nbt_0(Entity entity) {
        return NbtUtils.toPetPet(entity.writeNbt(new NbtCompound()));
    }
    @PetPetWhitelist
    public static Object nbt_1(Entity entity, PetPetList<Object> path) {
        return NbtUtils.getWithPath(entity.writeNbt(new NbtCompound()), path);
    }
    @PetPetWhitelist
    public static Entity vehicle(Entity entity) {
        return entity.getVehicle();
    }
    @PetPetWhitelist
    public static DimensionType dimension(Entity entity) {
        return WorldAPI.dimension((ClientWorld) entity.getWorld());
    }

    /**
     * Get the aspect for this entity.
     * Aspects obtained this way *do not*
     * have write access.
     */
    @PetPetWhitelist
    public static AspectAPI aspect(Entity entity) {
        Aspect foundAspect = AspectManager.getAspect(entity);
        if (foundAspect == null) return null;
        return new AspectAPI(foundAspect, false);
    }

    @PetPetWhitelist
    public static PetPetListView<Entity> passengers(Entity entity) {
        return new PetPetListView<>(entity.getPassengerList());
    }
    //@PetPetWhitelist
    public static BlockState targetedBlock(Entity entity) {
        throw new UnsupportedOperationException("Cannot call getTargetedBlock, unimplemented"); //advanced
    }
    //@PetPetWhitelist
    public static Entity targetedEntity(Entity entity) {
        throw new UnsupportedOperationException("Cannot call getTargetedEntity, unimplemented"); //advanced
    }

    /**
     * extra
     */

    @PetPetWhitelist
    public static String __tostring(Entity entity) {
        return (entity.hasCustomName() ? entity.getCustomName().getString() + "(" + type(entity) + ")" : type(entity)) + " (Entity)";
    }
    @PetPetWhitelist
    public static boolean isFox(Entity entity) {
        return entity instanceof FoxEntity || GroupUtils.is(entity.getUuid(), "fox");
    }
    @PetPetWhitelist
    public static boolean isCat(Entity entity) {
        return entity instanceof CatEntity || entity instanceof OcelotEntity || GroupUtils.is(entity.getUuid(), "cat");
    }
    @PetPetWhitelist
    public static boolean isBunny(Entity entity) {
        return entity instanceof RabbitEntity || GroupUtils.is(entity.getUuid(), "bunny");
    }
    @PetPetWhitelist
    public static boolean isCookie(Entity entity) {
        return (entity instanceof ItemEntity item && item.getStack().isOf(Items.COOKIE)) || GroupUtils.is(entity.getUuid(), "cookie");
    }
    @PetPetWhitelist
    public static boolean isFish(Entity entity) {
        return (entity instanceof FishEntity && !(entity instanceof TadpoleEntity)) || GroupUtils.is(entity.getUuid(), "fish");
    }
    @PetPetWhitelist
    public static boolean isBird(Entity entity) {
        return entity instanceof ParrotEntity || entity instanceof EnderDragonEntity || GroupUtils.is(entity.getUuid(), "bird");
    }
    @PetPetWhitelist
    public static boolean isHamburger(Entity entity) {
        return GroupUtils.is(entity.getUuid(), "hamburger");
    }

}
