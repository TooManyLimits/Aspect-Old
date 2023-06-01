package io.github.moonlightmaya.script.apis.world;

import io.github.moonlightmaya.util.ColorUtils;
import io.github.moonlightmaya.util.NbtUtils;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.joml.Vector4d;
import petpet.external.PetPetWhitelist;
import petpet.types.PetPetList;
import petpet.types.PetPetTable;

@PetPetWhitelist
public class ItemStackAPI {

    @PetPetWhitelist
    public static String id(ItemStack itemStack) {
        return Registries.ITEM.getId(itemStack.getItem()).toString();
    }

    /**
     * Skips creating the *entire big* NBT object when you only
     * want to check a specific path of the NBT.
     * If something goes wrong (trying to index a nonexistent
     * path, or indexing with an invalid object) returns null.
     */
    @PetPetWhitelist
    public static Object nbt_1(ItemStack itemStack, PetPetList<Object> path) {
        return NbtUtils.getWithPath(itemStack.getNbt(), path);
    }
    @PetPetWhitelist
    public static PetPetTable<String, Object> nbt_0(ItemStack itemStack) {
        return NbtUtils.toPetPet(itemStack.getNbt());
    }
    @PetPetWhitelist
    public static PetPetList<String> tags(ItemStack stack) {
        PetPetList<String> list = new PetPetList<>();
        stack.streamTags().map(TagKey::id).map(Identifier::toString).forEach(list::add);
        return list;
    }
    @PetPetWhitelist
    public static double miningSpeedFor(ItemStack stack, BlockState blockState) {
        return stack.getMiningSpeedMultiplier(blockState);
    }
    @PetPetWhitelist
    public static double maxCount(ItemStack stack) {
        return stack.getMaxCount();
    }
    @PetPetWhitelist
    public static double count(ItemStack stack) {
        return stack.getCount();
    }
    @PetPetWhitelist
    public static boolean stackable(ItemStack stack) {
        return stack.isStackable();
    }
    @PetPetWhitelist
    public static boolean damageable(ItemStack stack) {
        return stack.isDamageable();
    }
    @PetPetWhitelist
    public static double damage(ItemStack stack) {
        return stack.getDamage();
    }
    @PetPetWhitelist
    public static boolean durabilityBarVisible(ItemStack stack) {
        return stack.isItemBarVisible();
    }
    /**
     * 0 to 13
     */
    @PetPetWhitelist
    public static double durabilityBarAmount(ItemStack stack) {
        return stack.getItemBarStep();
    }
    @PetPetWhitelist
    public static Vector4d durabilityBarColor(ItemStack stack) {
        return ColorUtils.intARGBToVec(stack.getItemBarColor());
    }
    @PetPetWhitelist
    public static boolean isSuitableFor(ItemStack stack, BlockState block) {
        return stack.isSuitableFor(block);
    }
    @PetPetWhitelist
    public static double maxUseTime(ItemStack stack) {
        return stack.getMaxUseTime();
    }
    /**
     * NONE, EAT, DRINK, BLOCK, BOW, SPEAR, CROSSBOW, SPYGLASS, TOOT_HORN, BRUSH
     */
    @PetPetWhitelist
    public static String useAction(ItemStack stack) {
        return stack.getUseAction().toString();
    }
    @PetPetWhitelist
    public static boolean usedOnRelease(ItemStack stack) {
        return stack.isUsedOnRelease();
    }
    @PetPetWhitelist
    public static boolean hasNbt(ItemStack stack) {
        return stack.hasNbt();
    }
    @PetPetWhitelist
    public static boolean hasGlint(ItemStack stack) {
        return stack.hasGlint();
    }
    @PetPetWhitelist
    public static boolean hasCustomName(ItemStack stack) {
        return stack.hasCustomName();
    }

    /**
     * Not too sure about this one
     */
    @PetPetWhitelist
    public static String name(ItemStack stack) {
        return Text.Serializer.toJson(stack.getName());
    }

    /**
     * COMMON, UNCOMMON, RARE, EPIC
     */
    @PetPetWhitelist
    public static String rarity(ItemStack stack) {
        return stack.getRarity().name();
    }

    /**
     * Color of the rarity
     */
    @PetPetWhitelist
    public static Vector4d rarityColor(ItemStack stack) {
        int color = stack.getRarity().formatting.getColorValue();
        return ColorUtils.intARGBToVec(color);
    }
    @PetPetWhitelist
    public static boolean enchantable(ItemStack stack) {
        return stack.isEnchantable();
    }
    @PetPetWhitelist
    public static boolean hasEnchantments(ItemStack stack) {
        return stack.hasEnchantments();
    }
    @PetPetWhitelist
    public static Entity getHolder(ItemStack stack) {
        return stack.getHolder();
    }
    @PetPetWhitelist
    public static double repairCost(ItemStack stack) {
        return stack.getRepairCost();
    }
    @PetPetWhitelist
    public static boolean isFood(ItemStack stack) {
        return stack.isFood();
    }

    @PetPetWhitelist
    public static String __tostring(ItemStack stack) {
        return "ItemStack(" + stack.toString() + ")";
    }

}