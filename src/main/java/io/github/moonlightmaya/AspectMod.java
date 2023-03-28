package io.github.moonlightmaya;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import io.github.moonlightmaya.data.BaseStructures;
import io.github.moonlightmaya.data.importing.AspectImporter;
import io.github.moonlightmaya.manage.AspectManager;
import io.github.moonlightmaya.util.DisplayUtils;
import io.github.moonlightmaya.util.IOUtils;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
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
                Entity player = MinecraftClient.getInstance().player;
                AspectManager.loadAspectFromFolder(player, IOUtils.getOrCreateModFolder().resolve("test_aspect"),
                        t -> DisplayUtils.displayError("Failed to load test avatar", t, true));
                return 1;
            });
            aspect.then(test);

            LiteralArgumentBuilder<FabricClientCommandSource> put = literal("put");
            RequiredArgumentBuilder<FabricClientCommandSource, String> arg = RequiredArgumentBuilder.argument("aspect_name", StringArgumentType.greedyString());
            arg.executes(context -> {
                Entity target = context.getSource().getClient().targetedEntity;
                if (target != null) {
                    String name = StringArgumentType.getString(context, "aspect_name");
                    context.getSource().sendFeedback(Text.literal("Applying to " + target.getName()));
                    AspectManager.loadAspectFromFolder(target, IOUtils.getOrCreateModFolder().resolve(name),
                            t -> DisplayUtils.displayError("Failed to load test avatar", t, true));
                    return 1;
                } else {
                    context.getSource().sendError(Text.literal("No entity found"));
                    return 0;
                }
            });
            put.then(arg);
            aspect.then(put);

            LiteralArgumentBuilder<FabricClientCommandSource> putall = literal("putall");
            RequiredArgumentBuilder<FabricClientCommandSource, String> arg2 = RequiredArgumentBuilder.argument("aspect_name", StringArgumentType.greedyString());
            arg2.executes(context -> {
                for (Entity target : MinecraftClient.getInstance().world.getEntities()) {
                    String name = StringArgumentType.getString(context, "aspect_name");
                    context.getSource().sendFeedback(Text.literal("Applying to all entities"));
                    AspectManager.loadAspectFromFolder(target, IOUtils.getOrCreateModFolder().resolve(name),
                            t -> DisplayUtils.displayError("Failed to load test avatar", t, true));
                }
                return 1;
            });
            putall.then(arg2);
            aspect.then(putall);

            LiteralArgumentBuilder<FabricClientCommandSource> clear = literal("clear");
            clear.executes(context -> {
                AspectManager.clearAllAspects();
                return 1;
            });
            aspect.then(clear);

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



}
