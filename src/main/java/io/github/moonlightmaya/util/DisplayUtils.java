package io.github.moonlightmaya.util;

import io.github.moonlightmaya.AspectMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class DisplayUtils {

    public static void displayError(String message, Throwable error, boolean reportToChat) {
        AspectMod.LOGGER.error(message + ": ", error);
        if (reportToChat)
            MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(
                    Text.literal(message + ": " + error)
                            .formatted(Formatting.RED)
            );
    }


}
