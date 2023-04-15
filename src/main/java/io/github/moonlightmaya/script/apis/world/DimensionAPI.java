package io.github.moonlightmaya.script.apis.world;

import petpet.external.PetPetReflector;
import petpet.external.PetPetWhitelist;
import petpet.lang.run.PetPetClass;

@PetPetWhitelist
public class DimensionAPI {

    public static final PetPetClass DIMENSION_CLASS;

    static {
        DIMENSION_CLASS = PetPetReflector.reflect(DimensionAPI.class, "Dimension");
    }

}
