package io.github.moonlightmaya.script.apis.world;

import petpet.external.PetPetReflector;
import petpet.external.PetPetWhitelist;
import petpet.lang.run.PetPetClass;

@PetPetWhitelist
public class BiomeAPI {

    public static final PetPetClass BIOME_CLASS;

    static {
        BIOME_CLASS = PetPetReflector.reflect(BiomeAPI.class, "Biome");
    }

}
