package io.github.moonlightmaya.script.initializers;

import io.github.moonlightmaya.Aspect;
import io.github.moonlightmaya.model.AspectModelPart;
import io.github.moonlightmaya.util.DisplayUtils;
import petpet.external.PetPetInstance;
import petpet.external.PetPetReflector;

/**
 * Class with methods to create new instances of PetPet scripts
 * for use in Aspects.
 * The code in here will be run for *all* different Aspects,
 * and other classes will extend this one to add in functionality
 * for distinct aspect sandboxing situations.
 * - An initializer for Host Aspects
 * - An initializer for Remote Aspects
 * - An initializer for GUI Aspects
 * - And more if they come up
 */
public class ScriptInitializer {

    protected final Aspect aspect;

    public ScriptInitializer(Aspect aspect) {
        this.aspect = aspect;
    }

    public PetPetInstance createInstance() {
        PetPetInstance instance = new PetPetInstance();

        //instance.registerClass(WorldRootModelPart.class, PetPetReflector.reflect(WorldRootModelPart.class, "WorldRootModelPart"));
        instance.registerClass(AspectModelPart.class, PetPetReflector.reflect(AspectModelPart.class, "ModelPart"));

        instance.setGlobal("models", aspect.entityRoot);

        //Register print/log function
        //Remote aspects will likely want to remove these, but that's okay
        instance.setGlobal("print", DisplayUtils.PRINT_FUNCTION);
        instance.setGlobal("log", DisplayUtils.PRINT_FUNCTION);

        return instance;
    }


}
