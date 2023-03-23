package io.github.moonlightmaya;

import io.github.moonlightmaya.conversion.BaseStructures;
import io.github.moonlightmaya.conversion.importing.AspectImporter;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemGroups;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import petpet.external.PetPetInstance;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * The client mod initializer for Aspect, the core of the mod.
 * One of the myriad things we hoped to achieve with Aspect was
 * to improve organization of the codebase. To that end, I strive
 * to write plenty of documentation in the form of these "/**"
 * comments. Plus they're a bright green in IntelliJ which makes
 * them look really pretty :3
 */
public class AspectMod implements ClientModInitializer {

    /**
     * The mod id. Hopefully this isn't taken?
     * I found a mod online with the id "aspects", but this is slightly different.
     * And hey, it's not a bad thing to use a similar name to someone else.
     * The name is really cool, after all!
     */
    public static final String MODID = "aspect";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    //Testing variables
    public static Aspect TEST_ASPECT;

    @Override
    public void onInitializeClient() {
        LOGGER.info("Hello Aspect!");

        String source = "print(\"Hello Aspect from PetPet!\")";
        try {
            new PetPetInstance().runScript("script", source);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public static Identifier id(String path) {
        return new Identifier(MODID, path);
    }

    public static Path getModFolder() {
        Path path = FabricLoader.getInstance().getGameDir().resolve(MODID);
        if (Files.notExists(path)) {
            try {
                Files.createDirectory(path);
                LOGGER.info("Successfully created mod folder at " + path);
            } catch (IOException e) {
                LOGGER.error("Failed to create mod folder at " + path + ". Reason: ", e);
                return null;
            }
        } else {
            LOGGER.info("Located mod folder at " + path);
        }
        return path;
    }


    /**
     * Private testing methods that create a basic aspect,
     * save it in the static test variables, and update it each frame.
     */
    public static void createTestAspect() throws Throwable {
        //ignore null pointer for testing method
        Path searchPath = getModFolder().resolve("test_aspect");
        CompletableFuture<BaseStructures.AspectStructure> importTask = new AspectImporter(searchPath).doImport();
        BaseStructures.AspectStructure structure = importTask.get();
        TEST_ASPECT = new Aspect(UUID.randomUUID(), structure);
    }

    public static void updateTestAspect() {

    }
}
