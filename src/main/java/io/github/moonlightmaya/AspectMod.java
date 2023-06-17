package io.github.moonlightmaya;

import io.github.moonlightmaya.game_interfaces.AspectCommand;
import io.github.moonlightmaya.game_interfaces.AspectConfig;
import io.github.moonlightmaya.game_interfaces.AspectKeybinds;
import io.github.moonlightmaya.manage.AspectManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

        //Setup global ticking
        ClientTickEvents.START_WORLD_TICK.register(AspectManager::tick);
        //At the start and when leaving a world, clear all aspects
        AspectManager.clearAllAspects();
        ClientPlayConnectionEvents.DISCONNECT.register((networkHandler, client) -> AspectManager.clearAllAspects());

        //Setup game interfaces
        AspectConfig.load();
        AspectCommand.registerCommand();
        AspectKeybinds.registerKeybinds();

        //Once game actually begins, load gui aspect
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> AspectManager.reloadGuiAspect());
    }

    public static Identifier id(String path) {
        return new Identifier(MODID, path);
    }

}
