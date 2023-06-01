package io.github.moonlightmaya.script.apis.world;

import net.minecraft.block.Block;
import net.minecraft.client.render.DimensionEffects;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.world.dimension.DimensionType;
import org.jetbrains.annotations.Nullable;
import petpet.external.PetPetReflector;
import petpet.external.PetPetWhitelist;
import petpet.lang.run.PetPetClass;
import petpet.types.PetPetList;

import java.util.List;
import java.util.Optional;

@PetPetWhitelist
public class DimensionAPI {

    @PetPetWhitelist
    public static boolean hasSkyLight(DimensionType dimension) {
        return dimension.hasSkyLight();
    }

    @PetPetWhitelist
    public static boolean hasCeiling(DimensionType dimension) {
        return dimension.hasCeiling();
    }

    @PetPetWhitelist
    public static boolean ultrawarm(DimensionType dimension) {
        return dimension.ultrawarm();
    }

    @PetPetWhitelist
    public static boolean natural(DimensionType dimension) {
        return dimension.natural();
    }

    @PetPetWhitelist
    public static double coordinateScale(DimensionType dimension) {
        return dimension.coordinateScale();
    }

    @PetPetWhitelist
    public static boolean bedWorks(DimensionType dimension) {
        return dimension.bedWorks();
    }

    @PetPetWhitelist
    public static boolean respawnAnchorWorks(DimensionType dimension) {
        return dimension.hasSkyLight();
    }

    @PetPetWhitelist
    public static double minY(DimensionType dimension) {
        return dimension.minY();
    }

    @PetPetWhitelist
    public static double maxY(DimensionType dimension) {
        return dimension.height() + dimension.minY();
    }

    @PetPetWhitelist
    public static double height(DimensionType dimension) {
        return dimension.height();
    }

    /**
     * Idk what this is for 100%, looks like it's for
     * stuff like portal calculations and teleporting?
     * Guessing it's for stuff like the nether ceiling,
     * even though there's blocks above the nether ceiling
     * you can't normally get there.
     */
    @PetPetWhitelist
    public static double logicalHeight(DimensionType dimension) {
        return dimension.logicalHeight();
    }

    /**
     * All blocks which burn infinitely in this dimension
     */
    @PetPetWhitelist
    public static PetPetList<String> infiniburn(DimensionType dimension) {
        TagKey<Block> infiniburnBlocks = dimension.infiniburn();
        //This feels really clunky but i couldn't find a simpler way
        PetPetList<String> result = new PetPetList<>();
        Registries.BLOCK.getEntryList(infiniburnBlocks).ifPresent(x -> x.stream()
                .map(RegistryEntry::getKey)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(RegistryKey::getValue)
                .map(Identifier::toString)
                .forEachOrdered(result::add));
        return result;
    }

    @PetPetWhitelist @Nullable
    public static DimensionEffects effects(DimensionType dimension) {
        return DimensionEffects.byDimensionType(dimension);
    }

    @PetPetWhitelist
    public static float ambientLight(DimensionType dimension) {
        return dimension.ambientLight();
    }

    @PetPetWhitelist
    public static boolean piglinSafe(DimensionType dimension) {
        return dimension.monsterSettings().piglinSafe();
    }

    @PetPetWhitelist
    public static boolean hasRaids(DimensionType dimension) {
        return dimension.monsterSettings().hasRaids();
    }

    /**
     * The maximum light level at which mobs can spawn in this
     * dimension
     */
    @PetPetWhitelist
    public static boolean maxMobLight(DimensionType dimension) {
        return dimension.monsterSettings().piglinSafe();
    }

    @PetPetWhitelist
    public static String __tostring(DimensionType dimension) {
        return "Dimension(effects=" + dimension.effects() + ")";
    }

}
