package io.github.moonlightmaya.util;

import com.google.gson.JsonParser;
import io.github.moonlightmaya.AspectMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import petpet.lang.run.Interpreter;
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
        if (reportToChat) {
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
            Text result = Text.Serializer.fromLenientJson(s);
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
        Text text = Text.empty()
                .append(Text.literal("[PetPet] ").formatted(Formatting.AQUA))
                .append(Text.literal(message));
        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(text);
    }


}
