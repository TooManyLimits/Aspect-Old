package io.github.moonlightmaya.util;

import net.minecraft.nbt.*;
import petpet.types.PetPetList;
import petpet.types.PetPetTable;

public class NbtUtils {

    public static PetPetTable<String, Object> toPetPet(NbtCompound compound) {
        PetPetTable<String, Object> result = new PetPetTable<>();
        for (String s : compound.getKeys()) {
            result.put(s, toPetPet(compound.get(s)));
        }
        return result;
    }

    public static Object toPetPet(NbtElement element) {
        if (element instanceof AbstractNbtNumber num) {
            return num.doubleValue();
        } else if (element instanceof NbtString string) {
            return string.asString();
        } else if (element instanceof NbtCompound compound) {
            return toPetPet(compound);
        } else if (element instanceof AbstractNbtList<?> list) {
            PetPetList<Object> result = new PetPetList<>();
            for (NbtElement elem : list)
                result.add(toPetPet(elem));
            return result;
        }
        return null;
    }

}
