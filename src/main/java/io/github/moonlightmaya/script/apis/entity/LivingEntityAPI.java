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
    public static boolean riptideSpinning(LivingEntity livingEntity) {
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
    public static double getHealth(LivingEntity livingEntity) {
        return livingEntity.getHealth();
    }
    @PetPetWhitelist
    public static double getMaxHealth(LivingEntity livingEntity) {
        return livingEntity.getMaxHealth();
    }
    @PetPetWhitelist
    public static double getArmor(LivingEntity livingEntity) {
        return livingEntity.getArmor();
    }
    @PetPetWhitelist
    public static double getAbsorptionAmount(LivingEntity livingEntity) {
        return livingEntity.getAbsorptionAmount();
    }
    @PetPetWhitelist
    public static double getDeathTime(LivingEntity livingEntity) {
        return livingEntity.deathTime;
    }
    @PetPetWhitelist
    public static double getActiveItemTime(LivingEntity livingEntity) {
        return livingEntity.getItemUseTime();
    }
    @PetPetWhitelist
    public static double getBodyYaw_0(LivingEntity livingEntity) {
        return livingEntity.getBodyYaw();
    }
    @PetPetWhitelist
    public static double getBodyYaw_1(LivingEntity livingEntity, double delta) {
        return livingEntity.prevBodyYaw + (livingEntity.getBodyYaw() - livingEntity.prevBodyYaw) * delta;
    }

    /**
     * hand
     */
    @PetPetWhitelist
    public static boolean isSwingingArm(LivingEntity livingEntity) {
        return livingEntity.handSwinging;
    }
    @PetPetWhitelist
    public static double getSwingDuration(LivingEntity livingEntity) {
        throw new UnsupportedOperationException("Cannot call getSwingDuration"); //Needs mixin accessor
    }
    @PetPetWhitelist
    public static String getSwingArm(LivingEntity livingEntity) {
        return livingEntity.handSwinging ? livingEntity.preferredHand.name() : null;
    }
    @PetPetWhitelist
    public static boolean isLeftHanded(LivingEntity livingEntity) {
        return livingEntity.getMainArm() == Arm.LEFT;
    }
    @PetPetWhitelist
    public static int getSwingTime(LivingEntity livingEntity) {
        return livingEntity.handSwingTicks;
    }
    @PetPetWhitelist
    public static String getActiveHand(LivingEntity livingEntity) {
        return livingEntity.getActiveHand().name();
    }

    /**
     * string
     */
    @PetPetWhitelist
    public static String getEntityCategory(LivingEntity livingEntity) {
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
    public static ItemStack getHeldItem_0(LivingEntity livingEntity) {
        return livingEntity.getMainHandStack();
    }
    @PetPetWhitelist
    public static ItemStack getHeldItem_1(LivingEntity livingEntity, boolean offhand) {
        return offhand ? livingEntity.getOffHandStack() : livingEntity.getMainHandStack();
    }
    @PetPetWhitelist
    public static ItemStack getActiveItem(LivingEntity livingEntity) {
        return ItemUtils.checkStack(livingEntity.getActiveItem());
    }

    /**
     * almost useless
     */
    @PetPetWhitelist
    public static double getArrowCount(LivingEntity livingEntity) {
        return livingEntity.getStuckArrowCount();
    }
    @PetPetWhitelist
    public static double getStingerCount(LivingEntity livingEntity) {
        return livingEntity.getStingerCount();
    }

    /**
     * extra
     */

    @PetPetWhitelist
    public String __tostring(LivingEntity livingEntity) {
        return (livingEntity.hasCustomName() ? livingEntity.getCustomName().getString() + "(" + EntityAPI.getType(livingEntity) + ")" : EntityAPI.getType(livingEntity)) + " (LivingEntity)";
    }

}
