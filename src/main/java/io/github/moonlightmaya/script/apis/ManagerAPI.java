package io.github.moonlightmaya.script.apis;

import com.google.gson.Gson;
import io.github.moonlightmaya.AspectMetadata;
import io.github.moonlightmaya.AspectMod;
import io.github.moonlightmaya.data.importing.JsonStructures;
import io.github.moonlightmaya.manage.AspectManager;
import io.github.moonlightmaya.util.DisplayUtils;
import io.github.moonlightmaya.util.EntityUtils;
import io.github.moonlightmaya.util.IOUtils;
import org.jetbrains.annotations.Nullable;
import petpet.external.PetPetWhitelist;
import petpet.lang.run.PetPetException;
import petpet.types.PetPetList;
import petpet.types.PetPetTable;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * The manager API is used for management of the Aspect mod itself.
 * Its powers include:
 * - Ability to get a list of all Aspect file paths from your .minecraft/aspect/aspects folder
 * - Ability to interact with the mod settings (future)
 * - Ability to interact with settings of aspects (future)
 * - Ability to load and equip aspects from your aspects folder
 * Obviously these are powerful behaviors, so the Manager API will only be
 * available inside a GUI aspect. Users will be warned when applying a custom gui aspect
 * that it will be able to do these things, and that they should make sure that where
 * they got this GUI aspect is trustworthy.
 */
@PetPetWhitelist
public class ManagerAPI {

    /**
     * Returns a table where the keys are string file names.
     * The values are one of:
     * - Another such table, if the file is a non-aspect subfolder
     * - An AspectMetadata instance for the aspect in this file
     */
    @PetPetWhitelist
    public PetPetTable<String, Object> scanAspectsFolder() {
        return getAspectPathsRecursive(IOUtils.getOrCreateModFolder().resolve("aspects"));
    }

    private PetPetTable<String, Object> getAspectPathsRecursive(Path root) {
        PetPetTable<String, Object> res = new PetPetTable<>();
        List<File> folders = IOUtils.getSubFolders(root);
        for (File f : folders) {
            Path json = f.toPath().resolve("aspect.json");
            if (Files.exists(json) && !json.toFile().isDirectory()) {
                res.put(f.getName(), getMetadata(json));
            } else {
                //This folder is not itself an aspect, so recursively search for aspects inside
                PetPetTable<String, Object> subAspects = getAspectPathsRecursive(f.toPath());
                if (!subAspects.isEmpty())
                    res.put(f.getName(), subAspects);
            }
        }
        return res;
    }

    private @Nullable AspectMetadata getMetadata(Path aspectJson) {
        try {
            JsonStructures.Metadata json = new Gson().fromJson(Files.readString(aspectJson), JsonStructures.Metadata.class);
            return new AspectMetadata(json.toBaseStructure());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Accepts a path to an aspect in the form of a list.
     * Example for loading the aspect at
     * .minecraft/aspect/aspects/my_funny/aspect2/aspect.json
     * !["my_funny", "aspect2"]. Done
     */
    @PetPetWhitelist
    public void equipAspect(PetPetList<String> path) {
        Path orig = IOUtils.getOrCreateModFolder().resolve("aspects");
        Path cur = orig;
        for (String s : path) {
            cur = cur.resolve(s);
            if (!cur.normalize().startsWith(orig.normalize()))
                throw new PetPetException("Attempt to access aspect file outside of aspects/ folder?");
        }
        if (cur.equals(orig))
            throw new PetPetException("Cannot use mod_folder/aspects as an aspect folder");

        AspectManager.loadAspectFromFolder(EntityUtils.getLocalUUID(), cur, err -> {
            DisplayUtils.displayError("Failed to load aspect", err, true);
        });
    }

}
