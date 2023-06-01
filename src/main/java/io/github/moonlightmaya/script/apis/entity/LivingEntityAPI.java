package io.github.moonlightmaya.script.apis.entity;

import io.github.moonlightmaya.util.ItemUtils;
import net.minecraft.entity.EntityGroup;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import petpet.external.PetPetReflector;
import petpet.external.PetPetWhitelist;
import petpet.lang.run.PetPetClass;

@PetPetWhitelist
public class LivingEntityAPI {

    /**
     * boolean
     */
    @PetPetWhitelist
    public static boolean isUsingItem(LivingEntity livingEntity) {
        return livingEntity.isUsingItem();
    }

    @PetPetWhitelist
    public static boolean isSensitiveToWater(LivingEntity livingEntity) {
        return livingEntity.hurtByWater();
    }
    @PetPetWhitelist
    public static boolean isClimbing(LivingEntity livingEntity) {
        return livingEntity.isClimbing();
    }

    @PetPetWhitelist
    public static boolean isUsingRiptide(LivingEntity livingEntity) {
        return livingEntity.isUsingRiptide();
    }
    @PetPetWhitelist
    public static boolean isVisuallySwimming(LivingEntity livingEntity) {
        return livingEntity.isInSwimmingPose();
    }
    @PetPetWhitelist
    public static boolean isGliding(LivingEntity livingEntity) {
        return livingEntity.isFallFlying();
    }
    @PetPetWhitelist
    public static boolean isBlocking(LivingEntity livingEntity) {
        return livingEntity.isBlocking();
    }

    /**
     * number
     */
    @PetPetWhitelist
    public static double health(LivingEntity livingEntity) {
        return livingEntity.getHealth();
    }
    @PetPetWhitelist
    public static double maxHealth(LivingEntity livingEntity) {
        return livingEntity.getMaxHealth();
    }
    @PetPetWhitelist
    public static double armor(LivingEntity livingEntity) {
        return livingEntity.getArmor();
    }
    @PetPetWhitelist
    public static double absorption(LivingEntity livingEntity) {
        return livingEntity.getAbsorptionAmount();
    }
    @PetPetWhitelist
    public static double deathTime(LivingEntity livingEntity) {
        return livingEntity.deathTime;
    }
    @PetPetWhitelist
    public static double activeItemTime(LivingEntity livingEntity) {
        return livingEntity.getItemUseTime();
    }
    @PetPetWhitelist
    public static double bodyYaw_0(LivingEntity livingEntity) {
        return livingEntity.getBodyYaw();
    }
    @PetPetWhitelist
    public static double bodyYaw_1(LivingEntity livingEntity, double delta) {
        return livingEntity.prevBodyYaw + (livingEntity.getBodyYaw() - livingEntity.prevBodyYaw) * delta;
    }
    @PetPetWhitelist
    public static double armAngle_0(LivingEntity livingEntity) {
        return livingEntity.limbAnimator.getPos();
    }
    @PetPetWhitelist
    public static double armAngle_1(LivingEntity livingEntity, double delta) {
        return livingEntity.limbAnimator.getPos((float) delta);
    }

    /**
     * hand
     */
    @PetPetWhitelist
    public static boolean isSwingingArm(LivingEntity livingEntity) {
        return livingEntity.handSwinging;
    }
    @PetPetWhitelist
    public static double swingDuration(LivingEntity livingEntity) {
        throw new UnsupportedOperationException("Cannot call getSwingDuration"); //Needs mixin accessor
    }
    @PetPetWhitelist
    public static String swingArm(LivingEntity livingEntity) {
        return livingEntity.handSwinging ? livingEntity.preferredHand.name() : null;
    }
    @PetPetWhitelist
    public static boolean leftHanded(LivingEntity livingEntity) {
        return livingEntity.getMainArm() == Arm.LEFT;
    }
    @PetPetWhitelist
    public static int swingTime(LivingEntity livingEntity) {
        return livingEntity.handSwingTicks;
    }
    @PetPetWhitelist
    public static String activeHand(LivingEntity livingEntity) {
        return livingEntity.getActiveHand().name();
    }

    /**
     * string
     */
    @PetPetWhitelist
    public static String entityCategory(LivingEntity livingEntity) {
        if (livingEntity.getGroup() == EntityGroup.UNDEAD)
            return "UNDEAD";
        if (livingEntity.getGroup() == EntityGroup.ARTHROPOD)
            return "ARTHROPOD";
        if (livingEntity.getGroup() == EntityGroup.ILLAGER)
            return "ILLAGER";
        if (livingEntity.getGroup() == EntityGroup.AQUATIC)
            return "WATER";
        return "UNDEFINED";
    }

    /**
     * items
     */

    @PetPetWhitelist
    public static ItemStack heldItem_0(LivingEntity livingEntity) {
        return livingEntity.getMainHandStack();
    }
    @PetPetWhitelist
    public static ItemStack heldItem_1(LivingEntity livingEntity, boolean offhand) {
        return offhand ? livingEntity.getOffHandStack() : livingEntity.getMainHandStack();
    }
    @PetPetWhitelist
    public static ItemStack activeItem(LivingEntity livingEntity) {
        return ItemUtils.checkStack(livingEntity.getActiveItem());
    }

    /**
     * almost useless
     */
    @PetPetWhitelist
    public static double arrowCount(LivingEntity livingEntity) {
        return livingEntity.getStuckArrowCount();
    }
    @PetPetWhitelist
    public static double stingerCount(LivingEntity livingEntity) {
        return livingEntity.getStingerCount();
    }

    /**
     * extra
     */

    @PetPetWhitelist
    public String __tostring(LivingEntity livingEntity) {
        return (livingEntity.hasCustomName() ? livingEntity.getCustomName().getString() + "(" + EntityAPI.type(livingEntity) + ")" : EntityAPI.type(livingEntity)) + " (LivingEntity)";
    }

}
