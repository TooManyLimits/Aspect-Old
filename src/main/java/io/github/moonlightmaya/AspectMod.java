package io.github.moonlightmaya;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AspectMod implements ClientModInitializer {

    public static final String MODID = "aspect";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);
    public static Aspect TEST_ASPECT;


    @Override
    public void onInitializeClient() {
        LOGGER.info("Hello Aspect!");

        createTestAspect();
    }


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
