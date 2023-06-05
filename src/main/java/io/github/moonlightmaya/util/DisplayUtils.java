package io.github.moonlightmaya.util;

import com.google.gson.JsonParser;
import io.github.moonlightmaya.AspectMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class DisplayUtils {

    public static void displayError(String message, boolean reportToChat) {
        AspectMod.LOGGER.error(message);
        if (reportToChat && MinecraftClient.getInstance().inGameHud != null)
            MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(
                    Text.literal(message)
                            .formatted(Formatting.RED)
            );
    }

    public static void displayError(String message, Throwable error, boolean reportToChat) {
        AspectMod.LOGGER.error(message + ": ", error);
        if (reportToChat && MinecraftClient.getInstance().inGameHud != null) {
            String errorMessage = error.getMessage();
            while (errorMessage == null) {
                error = error.getCause();
                if (error != null)
                    errorMessage = error.getMessage();
                else
                    errorMessage = "Unknown cause, check console";
            }
            MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(
                    Text.literal(message + ": " + errorMessage)
                            .formatted(Formatting.RED)
            );
        }
    }

    public static Text tryParseJsonText(String s) {
        if (s == null) return Text.empty();
        try {
            JsonParser.parseString(s);
            Text result = Text.Serializer.fromJson(s);
            if (result == null) return Text.literal(s);
            return result;
        } catch (Exception e) {
            return Text.literal(s);
        }
    }

    /**
     * Displays the given message to chat with the PetPet header,
     * indicating the message is from the PetPet script.
     */
    public static void displayPetPetMessage(String message) {
        if (MinecraftClient.getInstance().inGameHud == null)
            return;

        Text text = Text.empty()
                .append(Text.literal("[PetPet] ").formatted(Formatting.AQUA))
                .append(Text.literal(message));
        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(text);
    }

    /**
     * Displays the message with the petpet header, except
     * with a Text object instead of a string, allowing
     * for formatting.
     */
    public static void displayPetPetMessage(Text message) {
        if (MinecraftClient.getInstance().inGameHud == null)
            return;

        Text text = Text.empty()
                .append(Text.literal("[PetPet] ").formatted(Formatting.AQUA))
                .append(message);
        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(text);
    }


}
