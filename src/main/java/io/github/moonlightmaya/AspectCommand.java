package io.github.moonlightmaya;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import io.github.moonlightmaya.manage.AspectManager;
import io.github.moonlightmaya.util.DisplayUtils;
import io.github.moonlightmaya.util.IOUtils;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import petpet.lang.run.PetPetException;

import java.nio.file.Path;

import static com.mojang.brigadier.builder.LiteralArgumentBuilder.literal;

/**
 * Everything to do with the `/aspect` command
 */
public class AspectCommand {

    public static void registerCommand() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            LiteralArgumentBuilder<FabricClientCommandSource> aspect = literal("aspect");
            aspect.then(equipCommand());
            aspect.then(putCommand());
            aspect.then(putAllCommand());
            aspect.then(clearCommand());
            aspect.then(runCommand());
            aspect.then(guiCommand());
            dispatcher.register(aspect);
        });
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> equipCommand() {
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
        return equip;
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> putCommand() {
        LiteralArgumentBuilder<FabricClientCommandSource> put = literal("put");
        RequiredArgumentBuilder<FabricClientCommandSource, String> arg = RequiredArgumentBuilder.argument("aspect_name", StringArgumentType.greedyString());
        arg.executes(context -> {
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
        put.then(arg);
        return put;
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> putAllCommand() {
        LiteralArgumentBuilder<FabricClientCommandSource> putall = literal("putall");
        RequiredArgumentBuilder<FabricClientCommandSource, String> arg = RequiredArgumentBuilder.argument("aspect_name", StringArgumentType.greedyString());
        arg.executes(context -> {
            context.getSource().sendFeedback(Text.literal("Applying to all entities"));
            String name = StringArgumentType.getString(context, "aspect_name");
            Path folder = IOUtils.getOrCreateModFolder().resolve(name);
            for (Entity target : MinecraftClient.getInstance().world.getEntities()) {
                AspectManager.loadAspectFromFolder(target.getUuid(), folder,
                        t -> DisplayUtils.displayError("Failed to load aspect", t, true));
            }
            return 1;
        });
        putall.then(arg);
        return putall;
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> clearCommand() {
        LiteralArgumentBuilder<FabricClientCommandSource> clear = literal("clear");
        clear.executes(context -> {
            AspectManager.clearAllAspects();
            return 1;
        });
        return clear;
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> runCommand() {
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
        return run;
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> guiCommand() {
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
        return gui;
    }

}
