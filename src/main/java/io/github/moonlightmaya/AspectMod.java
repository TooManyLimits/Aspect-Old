package io.github.moonlightmaya;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

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

        createTestAspect();
    }


    public static Identifier id(String path) {
        return new Identifier(MODID, path);
    }

    /**
     * Private testing methods that create a basic aspect,
     * save it in the static test variables, and update it each frame.
     */
    private static void createTestAspect() {

    }

    public static void updateTestAspect() {

    }
}
