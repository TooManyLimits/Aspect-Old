package io.github.moonlightmaya.script.apis;

import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import petpet.external.PetPetReflector;
import petpet.external.PetPetWhitelist;
import petpet.lang.run.PetPetClass;

@PetPetWhitelist
public class ItemStackAPI {

    public static final PetPetClass ITEMSTACK_CLASS;

    static {
        ITEMSTACK_CLASS = PetPetReflector.reflect(ItemStackAPI.class, "ItemStack");
    }

    @PetPetWhitelist
    public static String getId(ItemStack itemStack) {
        return Registries.ITEM.getId(itemStack.getItem()).toString();
    }

}