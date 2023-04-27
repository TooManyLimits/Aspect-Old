package io.github.moonlightmaya.script.apis;

import petpet.external.PetPetWhitelist;
import petpet.types.PetPetList;

/**
 * The manager API is used for management of the Aspect mod itself.
 * Its powers include:
 * - Ability to get a list of all Aspect file paths from your .minecraft/aspect folder
 * - Ability to interact with the mod settings
 * - Ability to interact with settings of aspects (config api-like)
 * - Ability to load aspects from your aspects folder
 * Obviously these are powerful behaviors, so the Manager API will only be
 * available inside a GUI aspect. Users will be warned when applying a custom gui aspect
 * that it will be able to do these things, and that they should make sure that where
 * they got this GUI aspect is trustworthy.
 */
@PetPetWhitelist
public class ManagerAPI {

    @PetPetWhitelist
    public PetPetList<String> getAspectPaths() {
        PetPetList<String> res = new PetPetList<>();
        return res;
    }




}
