package io.github.moonlightmaya;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AspectMod implements ClientModInitializer {

    public static final String MODID = "aspect";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);


    @Override
    public void onInitializeClient() {
        LOGGER.info("Hello Aspect!");
    }
}
