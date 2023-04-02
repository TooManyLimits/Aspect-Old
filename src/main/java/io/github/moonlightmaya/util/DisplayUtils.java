package io.github.moonlightmaya.util;

import io.github.moonlightmaya.AspectMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import petpet.lang.run.JavaFunction;
import petpet.types.PetPetString;

public class DisplayUtils {

    public static void displayError(String message, boolean reportToChat) {
        AspectMod.LOGGER.error(message);
        if (reportToChat)
            MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(
                    Text.literal(message)
                            .formatted(Formatting.RED)
            );
    }

    public static void displayError(String message, Throwable error, boolean reportToChat) {
        AspectMod.LOGGER.error(message + ": ", error);
        if (reportToChat)
            MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(
                    Text.literal(message + ": " + error)
                            .formatted(Formatting.RED)
            );
    }


    /**
     * Keep the print function in here
     */
    public static final JavaFunction PRINT_FUNCTION = new JavaFunction(DisplayUtils.class, "displayPetPetMessage", false);
    /**
     * Displays the given message to chat with the PetPet header,
     * indicating the message is from the PetPet script.
     */
    public static void displayPetPetMessage(Object message) {
        String string = PetPetString.valueOf(message);
        Text text = Text.empty()
                .append(Text.literal("[PetPet] ").formatted(Formatting.AQUA))
                .append(Text.literal(string));
        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(text);
    }


}
