package io.github.moonlightmaya.script.apis;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.LunarWorldView;
import net.minecraft.world.World;
import petpet.lang.run.JavaFunction;
import petpet.lang.run.PetPetCallable;
import petpet.lang.run.PetPetClass;

public class WorldAPI {

    public static final PetPetClass WORLD_CLASS = new PetPetClass("World");

    static {
        //Vanilla methods
        WORLD_CLASS.addMethod("getTime", new JavaFunction(World.class, "getTime", true));
        WORLD_CLASS.addMethod("getTimeOfDay", new JavaFunction(ClientWorld.class, "getTimeOfDay", true));
        WORLD_CLASS.addMethod("getDimension", new JavaFunction(World.class, "getDimension", true)); //Need to register DimensionType class
        WORLD_CLASS.addMethod("getLunarTime", new JavaFunction(LunarWorldView.class, "getLunarTime", true));
        WORLD_CLASS.addMethod("getMoonPhase", new JavaFunction(LunarWorldView.class, "getMoonPhase", true));

    }

}
