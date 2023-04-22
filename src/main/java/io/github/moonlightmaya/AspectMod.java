package io.github.moonlightmaya;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import io.github.moonlightmaya.manage.AspectManager;
import io.github.moonlightmaya.util.DisplayUtils;
import io.github.moonlightmaya.util.IOUtils;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import petpet.external.PetPetInstance;
import petpet.lang.run.PetPetException;

import java.nio.file.Path;

import static com.mojang.brigadier.builder.LiteralArgumentBuilder.literal;

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
        ClientTickEvents.START_WORLD_TICK.register(AspectManager::tick);

        //When leaving a world, clear all aspects
        ClientPlayConnectionEvents.DISCONNECT.register((networkHandler, client) -> {
            AspectManager.clearAllAspects();
        });

        //Register testing command
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            LiteralArgumentBuilder<FabricClientCommandSource> aspect = literal("aspect");

            LiteralArgumentBuilder<FabricClientCommandSource> equip = literal("equip");
            RequiredArgumentBuilder<FabricClientCommandSource, String> arg = RequiredArgumentBuilder.argument("aspect_name", StringArgumentType.greedyString());
            arg.executes(context -> {
                Entity player = MinecraftClient.getInstance().player;
                String name = StringArgumentType.getString(context, "aspect_name");
                AspectManager.loadAspectFromFolder(player.getUuid(), IOUtils.getOrCreateModFolder().resolve(name),
                        t -> DisplayUtils.displayError("Failed to load aspect", t, true));
                return 1;
            });
            equip.then(arg);
            aspect.then(equip);


            LiteralArgumentBuilder<FabricClientCommandSource> put = literal("put");
            RequiredArgumentBuilder<FabricClientCommandSource, String> arg2 = RequiredArgumentBuilder.argument("aspect_name", StringArgumentType.greedyString());
            arg2.executes(context -> {
                Entity target = context.getSource().getClient().targetedEntity;
                if (target != null) {
                    String name = StringArgumentType.getString(context, "aspect_name");
                    context.getSource().sendFeedback(Text.literal("Applying to " + target.getName()));
                    AspectManager.loadAspectFromFolder(target.getUuid(), IOUtils.getOrCreateModFolder().resolve(name),
                            t -> DisplayUtils.displayError("Failed to load aspect", t, true));
                    return 1;
                } else {
                    context.getSource().sendError(Text.literal("No entity found"));
                    return 0;
                }
            });
            put.then(arg2);
            aspect.then(put);

            LiteralArgumentBuilder<FabricClientCommandSource> putall = literal("putall");
            RequiredArgumentBuilder<FabricClientCommandSource, String> arg3 = RequiredArgumentBuilder.argument("aspect_name", StringArgumentType.greedyString());
            arg3.executes(context -> {
                context.getSource().sendFeedback(Text.literal("Applying to all entities"));
                String name = StringArgumentType.getString(context, "aspect_name");
                Path folder = IOUtils.getOrCreateModFolder().resolve(name);
                for (Entity target : MinecraftClient.getInstance().world.getEntities()) {
                    AspectManager.loadAspectFromFolder(target.getUuid(), folder,
                            t -> DisplayUtils.displayError("Failed to load aspect", t, true));
                }
                return 1;
            });
            putall.then(arg3);
            aspect.then(putall);

            LiteralArgumentBuilder<FabricClientCommandSource> clear = literal("clear");
            clear.executes(context -> {
                AspectManager.clearAllAspects();
                return 1;
            });
            aspect.then(clear);

            LiteralArgumentBuilder<FabricClientCommandSource> run = literal("run");
            RequiredArgumentBuilder<FabricClientCommandSource, String> code = RequiredArgumentBuilder.argument("code", StringArgumentType.greedyString());
            code.executes(context -> {
                Entity player = MinecraftClient.getInstance().player;
                String theCode = StringArgumentType.getString(context, "code");
                Aspect playerAspect = AspectManager.getAspect(player.getUuid());
                if (playerAspect == null)
                    DisplayUtils.displayError("Failed to run code, no active environment", true);
                else {
                    try {
                        Object result = playerAspect.scriptHandler.runCode("run", theCode);
                        if (result != null)
                            DisplayUtils.displayPetPetMessage(playerAspect.scriptHandler.getStringFor(result));
                    } catch (PetPetException petpetError) {
                        DisplayUtils.displayError(petpetError.getMessage(), true); //don't put the exception itself, just the error
                    } catch (Throwable t) {
                        DisplayUtils.displayError(t.getMessage(), t,true);
                    }
                }
                return 1;
            });
            run.then(code);
            aspect.then(run);

            LiteralArgumentBuilder<FabricClientCommandSource> gui = literal("gui");
            RequiredArgumentBuilder<FabricClientCommandSource, String> arg4 = RequiredArgumentBuilder.argument("aspect_name", StringArgumentType.greedyString());
            arg4.executes(context -> {
                context.getSource().sendFeedback(Text.literal("Setting GUI aspect"));
                String name = StringArgumentType.getString(context, "aspect_name");
                Path folder = IOUtils.getOrCreateModFolder().resolve(name);
                AspectManager.loadGuiAspect(folder, t -> DisplayUtils.displayError("Failed to load GUI aspect", t, true));
                return 1;
            });
            gui.then(arg4);
            aspect.then(gui);

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
