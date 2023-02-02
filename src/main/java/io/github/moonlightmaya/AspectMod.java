package io.github.moonlightmaya;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The client mod initializer for Aspect, the core of the mod.
 * One of the myriad things we hoped to achieve with Aspect was
 * to improve organization of the codebase. To that end, I strive
 * to write plenty of documentation in the form of these "/**"
 * comments. Plus they're a bright green in IntelliJ which makes
 * them look really pretty.
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
    public static Aspect TEST_ASPECT;


    @Override
    public void onInitializeClient() {
        LOGGER.info("Hello Aspect!");

        createTestAspect();
    }


    /**
     * Private testing method that creates a basic aspect, that's just
     * a singular quad, and saves it in the static test variable.
     */
    private static void createTestAspect() {
        AspectModelPart root = new AspectModelPart();
        root.vertexData = new float[] { //position, texture, normal
                0, 0, -5,  0, 0,  0, 0, 1,
                1, 0, -5,  1, 0,  0, 0, 1,
                1, 1, -5,  1, 1,  0, 0, 1,
                0, 1, -5,  0, 1,  0, 0, 1,
        };
        Aspect aspect = new Aspect();
        aspect.root = root;
        TEST_ASPECT = aspect;
    }
}
