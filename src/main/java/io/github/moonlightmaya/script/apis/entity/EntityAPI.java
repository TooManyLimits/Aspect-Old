package io.github.moonlightmaya.script.apis.entity;

import io.github.moonlightmaya.Aspect;
import io.github.moonlightmaya.manage.AspectManager;
import io.github.moonlightmaya.script.annotations.AllowIfHost;
import io.github.moonlightmaya.script.apis.AspectAPI;
import io.github.moonlightmaya.script.apis.world.WorldAPI;
import io.github.moonlightmaya.util.GroupUtils;
import io.github.moonlightmaya.util.MathUtils;
import io.github.moonlightmaya.util.NbtUtils;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.*;
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
import petpet.external.PetPetReflector;
import petpet.external.PetPetWhitelist;
import petpet.lang.run.JavaFunction;
import petpet.lang.run.PetPetClass;
import petpet.lang.run.PetPetException;
import petpet.types.PetPetTable;
import petpet.types.immutable.PetPetListView;

import java.util.Iterator;

/**
 * Basic functions shared by all entities
 */
@PetPetWhitelist
public class EntityAPI {

    public static final PetPetClass ENTITY_CLASS;

    static {
        ENTITY_CLASS = PetPetReflector.reflect(EntityAPI.class, "Entity");
    }

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
    public static int getPermissionLevel(Entity entity) {
        int i = 1;
        while (entity.hasPermissionLevel(i)) i++;
        return i - 1;
    }
    @PetPetWhitelist
    public static double getFrozenTicks(Entity entity) {
        return entity.getFrozenTicks();
    }
    @PetPetWhitelist
    public static double getMaxAir(Entity entity) {
        return entity.getMaxAir();
    }
    @PetPetWhitelist
    public static double getEyeHeight(Entity entity) {
        return entity.getEyeHeight(entity.getPose());
    }
    @PetPetWhitelist
    public static double getEyeY(Entity entity) {
        return entity.getEyeY();
    }

    /**
     * vector
     */
    @PetPetWhitelist
    public static Vector3d getPos_0(Entity entity) {
        return MathUtils.fromVec3d(entity.getPos());
    }
    @PetPetWhitelist
    public static Vector3d getPos_1(Entity entity, double delta) {
        return MathUtils.fromVec3d(entity.getLerpedPos((float) delta));
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
    public static Vector3d getVelocity(Entity entity) {
        return new Vector3d(
                entity.getX() - entity.prevX,
                entity.getY() - entity.prevY,
                entity.getZ() - entity.prevZ
        );
    }
    @PetPetWhitelist
    public static Vector3d getLookDir(Entity entity) {
        return MathUtils.fromVec3d(entity.getRotationVector());
    }
    @PetPetWhitelist
    public static Vector3d getBoundingBox(Entity entity) {
        EntityDimensions dim = entity.getDimensions(entity.getPose());
        return new Vector3d(dim.width, dim.height, dim.width);
    }


    /**
     * string
     */

    @PetPetWhitelist
    public static String getName(Entity entity) {
        return entity.getName().getString();
    }
    @PetPetWhitelist
    public static String getType(Entity entity) {
        return Registries.ENTITY_TYPE.getId(entity.getType()).toString();
    }
    @PetPetWhitelist
    public static String getPose(Entity entity) {
        return entity.getPose().name();
    }
    @PetPetWhitelist
    public static String getUUID(Entity entity) {
        return entity.getUuidAsString();
    }
    @PetPetWhitelist
    public static String getDimensionName(Entity entity) {
        return WorldAPI.getDimensionName((ClientWorld) entity.getWorld());
    }

    /**
     * other
     */
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
    public static Entity getVehicle(Entity entity) {
        return entity.getVehicle();
    }
    @PetPetWhitelist
    public static DimensionType getDimension(Entity entity) {
        return WorldAPI.getDimension((ClientWorld) entity.getWorld());
    }

    //@PetPetWhitelist
    public static PetPetListView<Entity> getPassengers(Entity entity) {
        //Wait for fix on allowing any list in a view
        throw new UnsupportedOperationException("Cannot call getPassengers, unimplemented");
//        return new PetPetListView<>(entity.getPassengerList());
    }
    //@PetPetWhitelist
    public static BlockState getTargetedBlock(Entity entity) {
        throw new UnsupportedOperationException("Cannot call getTargetedBlock, unimplemented"); //advanced
    }
    //@PetPetWhitelist
    public static Entity getTargetedEntity(Entity entity) {
        throw new UnsupportedOperationException("Cannot call getTargetedEntity, unimplemented"); //advanced
    }

    /**
     * extra
     */

    @PetPetWhitelist
    public static String __tostring(Entity entity) {
        return (entity.hasCustomName() ? entity.getCustomName().getString() + "(" + getType(entity) + ")" : getType(entity)) + " (Entity)";
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

    /**
     * The function(s) below this point have certain special properties.
     * What they do will be different depending on the interpreter that
     * calls the method, as well as the parameter.
     *
     * The getAspect() method returns an AspectAPI. This API is editable
     * if and only if the entity you are grabbing the aspect of is the same
     * as the aspect in which the script is running.
     *
     * Complex permissions like this require additional effort to set up,
     * so they are implemented as their own, more elaborate, methods.
     */
    public static JavaFunction getGetAspectMethod(Aspect aspectOwnedByScript) {
        return new JavaFunction(false, 1) {
            @Override
            public Object invoke(Object arg0) {
                if (arg0 instanceof Entity entity) {
                    Aspect foundAspect = AspectManager.getAspect(entity.getUuid());
                    if (foundAspect == null) return null;
                    return new AspectAPI(foundAspect, foundAspect == aspectOwnedByScript);
                } else throw new PetPetException("Cannot call Entity.getAspect() on non-entity?");
            }
        };
    }

}
