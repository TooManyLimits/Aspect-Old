package io.github.moonlightmaya;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.github.moonlightmaya.data.BaseStructures;
import io.github.moonlightmaya.data.importing.AspectImporter;
import io.github.moonlightmaya.manage.AspectManager;
import io.github.moonlightmaya.util.DisplayUtils;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import petpet.external.PetPetInstance;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static com.mojang.brigadier.builder.LiteralArgumentBuilder.*;

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

        //Setup global ticking objects
        ClientTickEvents.START_CLIENT_TICK.register(AspectManager::tick);

        //Register testing command
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            LiteralArgumentBuilder<FabricClientCommandSource> aspect = literal("aspect");
            LiteralArgumentBuilder<FabricClientCommandSource> test = literal("test");
            test.executes(context -> {
                context.getSource().sendFeedback(Text.literal("loading"));
                UUID clientUUID = MinecraftClient.getInstance().player.getUuid();
                AspectManager.loadAspectFromFolder(clientUUID, getOrCreateModFolder().resolve("test_aspect"),
                        t -> DisplayUtils.displayError("Failed to load test avatar", t, true));
                return 1;
            });
            aspect.then(test);
            dispatcher.register(aspect);
        });

        String script = "print(\"Hello Aspect from PetPet!\")";
        try {
            new PetPetInstance().runScript("script", script);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public static Identifier id(String path) {
        return new Identifier(MODID, path);
    }

    /**
     * Navigate to the mod folder and create it if it doesn't exist.
     * In the future, functionality will be added for a customizable
     * mod folder location.
     */
    public static Path getOrCreateModFolder() {
        Path path = FabricLoader.getInstance().getGameDir().resolve(MODID);
        if (Files.notExists(path)) {
            try {
                LOGGER.info("Did not find mod folder at " + path + ". Creating...");
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
        Path searchPath = getOrCreateModFolder().resolve("test_aspect");
        CompletableFuture<BaseStructures.AspectStructure> importTask = new AspectImporter(searchPath).doImport();

        //Test serialization and deserialization
        BaseStructures.AspectStructure structure = importTask.get();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(out);
        structure.write(dos);
        System.out.println(out.size() + " bytes serialized");
        ByteArrayInputStream bais = new ByteArrayInputStream(out.toByteArray());
        DataInputStream dis = new DataInputStream(bais);
        BaseStructures.AspectStructure reconstructed = BaseStructures.AspectStructure.read(dis);

        TEST_ASPECT = new Aspect(UUID.randomUUID(), reconstructed);
    }

    public static void updateTestAspect() {

    }
}
