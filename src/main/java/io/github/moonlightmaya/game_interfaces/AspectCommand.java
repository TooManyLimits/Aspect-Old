package io.github.moonlightmaya.game_interfaces;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.github.moonlightmaya.Aspect;
import io.github.moonlightmaya.manage.AspectManager;
import io.github.moonlightmaya.util.DisplayUtils;
import io.github.moonlightmaya.util.EntityUtils;
import io.github.moonlightmaya.util.IOUtils;
import io.github.moonlightmaya.vanilla.BBModelExporter;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.RegistryEntryArgumentType;
import net.minecraft.command.suggestion.SuggestionProviders;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import petpet.lang.run.PetPetException;

import java.io.IOException;
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
            aspect.then(exportCommand(registryAccess));
            dispatcher.register(aspect);
        });
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> equipCommand() {
        LiteralArgumentBuilder<FabricClientCommandSource> equip = literal("equip");
        RequiredArgumentBuilder<FabricClientCommandSource, String> arg = RequiredArgumentBuilder.argument("aspect_name", StringArgumentType.greedyString());
        arg.executes(context -> {
            String name = StringArgumentType.getString(context, "aspect_name");
            AspectManager.loadAspectFromPath(EntityUtils.getLocalUUID(), IOUtils.getOrCreateModFolder().resolve("aspects").resolve(name),
                    t -> DisplayUtils.displayError("Failed to load aspect", t, true), true, false);
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
                AspectManager.loadAspectFromPath(target.getUuid(), IOUtils.getOrCreateModFolder().resolve("aspects").resolve(name),
                        t -> DisplayUtils.displayError("Failed to load aspect", t, true), true, false);
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
            Path folder = IOUtils.getOrCreateModFolder().resolve("aspects").resolve(name);
            for (Entity target : MinecraftClient.getInstance().world.getEntities()) {
                AspectManager.loadAspectFromPath(target.getUuid(), folder,
                        t -> DisplayUtils.displayError("Failed to load aspect", t, true), true, false);
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
            Aspect localAspect = AspectManager.getAspect(EntityUtils.getLocalUUID());
            String theCode = StringArgumentType.getString(context, "code");
            if (localAspect == null)
                DisplayUtils.displayError("Failed to run code, no active environment", true);
            else {
                try {
                    Object result = localAspect.script.runCode("run", theCode);
                    if (result != null)
                        DisplayUtils.displayPetPetMessage(localAspect.script.getStringFor(result));
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
        gui.executes(context -> {
            context.getSource().sendFeedback(Text.literal("Resetting GUI aspect to default"));
            AspectConfig.GUI_PATH.set("");
            AspectManager.reloadGuiAspect();
            return 1;
        });
        RequiredArgumentBuilder<FabricClientCommandSource, String> arg4 = RequiredArgumentBuilder.argument("aspect_name", StringArgumentType.greedyString());
        arg4.executes(context -> {
            context.getSource().sendFeedback(Text.literal("Setting GUI aspect"));
            String name = StringArgumentType.getString(context, "aspect_name");
            AspectConfig.GUI_PATH.set(name);
            AspectManager.reloadGuiAspect();
            return 1;
        });
        gui.then(arg4);
        return gui;
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> exportCommand(CommandRegistryAccess registryAccess) {
        LiteralArgumentBuilder<FabricClientCommandSource> export = literal("export");

        // /export player <slim? true/false>
        LiteralArgumentBuilder<FabricClientCommandSource> player = literal("player");
        RequiredArgumentBuilder<FabricClientCommandSource, Boolean> slim = RequiredArgumentBuilder.argument("slim", BoolArgumentType.bool());
        slim.executes(context -> {
            boolean isSlim = BoolArgumentType.getBool(context, "slim");
            context.getSource().sendFeedback(Text.literal("Generating " + (isSlim ? "slim" : "default") + " player model..."));
            BBModelExporter exporter = BBModelExporter.player(isSlim);
            context.getSource().sendFeedback(Text.literal("Generated model. Saving..."));
            try {
                String fileName = isSlim ? "player_slim.bbmodel" : "player_default.bbmodel";
                Path p = IOUtils.getOrCreateModFolder().resolve("exported").resolve(fileName);
                exporter.save(p);
            } catch (IOException e) {
                context.getSource().sendError(Text.literal("An error occurred. Check Minecraft logs for details."));
                e.printStackTrace();
            }
            return 1;
        });
        player.then(slim);
        export.then(player);

        // /export entity <entity type>
        LiteralArgumentBuilder<FabricClientCommandSource> entity = literal("entity");
        RequiredArgumentBuilder<FabricClientCommandSource, RegistryEntry.Reference<EntityType<?>>> type =
                RequiredArgumentBuilder.argument("entity_type", RegistryEntryArgumentType.registryEntry(registryAccess, RegistryKeys.ENTITY_TYPE));
        type.executes(context -> {
            //Horrible cursed cast
            EntityType<?> entityType = (EntityType<?>) RegistryEntryArgumentType.getSummonableEntityType((CommandContext) context, "entity_type").value();
            context.getSource().sendFeedback(Text.literal("Generating entity model..."));
            BBModelExporter exporter = BBModelExporter.entity(entityType);
            context.getSource().sendFeedback(Text.literal("Generated model. Saving..."));
            try {
                String fileName = "generated_" + Registries.ENTITY_TYPE.getId(entityType).getPath() + ".bbmodel";
                Path p = IOUtils.getOrCreateModFolder().resolve("exported").resolve(fileName);
                exporter.save(p);
            } catch (IOException e) {
                context.getSource().sendError(Text.literal("An error occurred. Check Minecraft logs for details."));
                e.printStackTrace();
            }
            return 1;
        });
        entity.then(type);
        export.then(entity);

        return export;
    }

}
