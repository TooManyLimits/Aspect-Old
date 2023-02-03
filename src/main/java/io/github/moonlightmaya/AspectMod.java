package io.github.moonlightmaya;

import net.fabricmc.api.ClientModInitializer;
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
    public static AspectModelPart TEST_ROOT_PART;
    public static AspectModelPart TEST_ORBITER_PART;


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
                -1, 0, -1,  0, 0,  0, 0, 1,
                1, 0, -1,  1, 0,  0, 0, 1,
                1, 0, 1,  1, 1,  0, 0, 1,
                -1, 0, 1,  0, 1,  0, 0, 1,
        };
        AspectModelPart orbiter = new AspectModelPart();
        orbiter.vertexData = new float[] {
                -1, -1, 0,  0, 0,  0, 0, 1,
                1, -1, 0,  1, 0,  0, 0, 1,
                1, 1, 0,  1, 1,  0, 0, 1,
                -1, 1, 0,  0, 1,  0, 0, 1,
        };
        root.children = List.of(orbiter);
        Aspect aspect = new Aspect();
        aspect.entityRoot = root;
        TEST_ASPECT = aspect;
        TEST_ROOT_PART = root;
        TEST_ORBITER_PART = orbiter;
    }

    public static void updateTestAspect() {
        AspectModelPart root = TEST_ROOT_PART;
        AspectModelPart outer = TEST_ORBITER_PART;
        float time = (System.currentTimeMillis() % 10000) / 10000f;

        root.partPos.set(0, Math.sin(time * Math.PI * 2), 0);
        root.partPivot.set(0, Math.sin(time * Math.PI * 2), 0);
        root.partRot.rotationX((float) Math.toRadians(0));

        outer.partPos.set(0, 0, 1);
        outer.partPivot.set(0, 0, 1);
        outer.partRot.rotationY((float) Math.toRadians(30));

        outer.partScale.set(2, 5, 1);

        root.needsMatrixRecalculation = true;
        outer.needsMatrixRecalculation = true;
    }
}
