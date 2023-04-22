package io.github.moonlightmaya.script.apis;

import com.mojang.brigadier.StringReader;
import io.github.moonlightmaya.Aspect;
import io.github.moonlightmaya.util.DisplayUtils;
import io.github.moonlightmaya.util.EntityUtils;
import io.github.moonlightmaya.util.ItemUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.util.Session;
import net.minecraft.command.argument.ItemSlotArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import org.joml.Vector3d;
import petpet.external.PetPetWhitelist;
import petpet.lang.run.PetPetException;

@PetPetWhitelist
public class HostAPI {

    private final Aspect aspect;

    /**
     * Key boolean: this dictates whether the host api is able to perform
     * host-only actions. Such actions should only be callable from an
     * aspect that is host, and hence, the extension is made to store
     * whether this API is host.
     */
    private final boolean isHost;

    public boolean cursorUnlocked;

    public Vector3d chatColor = null;

    public HostAPI(Aspect aspect) {
        this.aspect = aspect;
        this.isHost = aspect.isHost;
    }

    @PetPetWhitelist
    public boolean isHost() {
        return isHost;
    }

    @PetPetWhitelist
    public HostAPI sendChatMessage(String text) {
        if (isHost) {
            ClientPlayNetworkHandler network = MinecraftClient.getInstance().getNetworkHandler();
            if (network != null) network.sendChatMessage(text);
        }
        return this;
    }

    @PetPetWhitelist
    public HostAPI sendChatCommand(String text) {
        if (isHost) {
            ClientPlayNetworkHandler network = MinecraftClient.getInstance().getNetworkHandler();
            if (network != null) network.sendChatCommand(text.startsWith("/") ? text.substring(1) : text);
        }
        return this;
    }

    @PetPetWhitelist
    public HostAPI appendChatHistory(String text) {
        if (isHost)
            MinecraftClient.getInstance().inGameHud.getChatHud().addToMessageHistory(text);
        return this;
    }

    @PetPetWhitelist
    public HostAPI swingArm(boolean useOffHand) {
        if (isHost && MinecraftClient.getInstance().player != null)
            MinecraftClient.getInstance().player.swingHand(useOffHand ? Hand.OFF_HAND : Hand.MAIN_HAND);
        return this;
    }

    @PetPetWhitelist
    public ItemStack getSlot(Object arg) {
        if (!isHost()) return null;

        Entity user = EntityUtils.getEntityByUUID(MinecraftClient.getInstance().world, this.aspect.userUUID);
        if (user == null)
            return null;

        Integer index;
        if (arg instanceof String s) {
            try {
                index = ItemSlotArgumentType.itemSlot().parse(new StringReader(s));
            } catch (Exception e) {
                throw new PetPetException("Unable to get slot \"" + s + "\"");
            }
        } else if (arg instanceof Integer i)
            index = i;
        else {
            throw new PetPetException("Invalid type for getSlot argument, expected number or string");
        }

        return ItemUtils.checkStack(user.getStackReference(index).get());
    }

    @PetPetWhitelist
    public Vector3d chatColor_0() {
        return new Vector3d(chatColor);
    }

    @PetPetWhitelist
    public HostAPI chatColor_1(Vector3d arg) {
        return chatColor_3(arg.x, arg.y, arg.z);
    }

    @PetPetWhitelist
    public HostAPI chatColor_3(double r, double g, double b) {
        if (isHost)
            this.chatColor = new Vector3d(r, g, b);
        return this;
    }

    @PetPetWhitelist
    public boolean isCursorUnlocked() {
        return cursorUnlocked;
    }

    @PetPetWhitelist
    public HostAPI unlockCursor() {
        if (isHost)
            cursorUnlocked = true;
        return this;
    }

    @PetPetWhitelist
    public HostAPI lockCursor() {
        if (isHost)
            cursorUnlocked = false;
        return this;
    }

    @PetPetWhitelist
    public HostAPI titleTimes_1(Vector3d vec) {
        if (isHost)
            MinecraftClient.getInstance().inGameHud.setTitleTicks((int) vec.x, (int) vec.y, (int) vec.z);
        return this;
    }

    @PetPetWhitelist
    public HostAPI titleTimes_3(double x, double y, double z) {
        if (isHost)
            MinecraftClient.getInstance().inGameHud.setTitleTicks((int) x, (int) y, (int) z);
        return this;
    }

    @PetPetWhitelist
    public HostAPI title_0() {
        if (isHost)
            MinecraftClient.getInstance().inGameHud.clearTitle();
        return this;
    }

    @PetPetWhitelist
    public HostAPI title_1(String text) {
        if (isHost)
            MinecraftClient.getInstance().inGameHud.setTitle(DisplayUtils.tryParseJsonText(text));
        return this;
    }

    @PetPetWhitelist
    public HostAPI subtitle(String text) {
        if (isHost)
            MinecraftClient.getInstance().inGameHud.setSubtitle(DisplayUtils.tryParseJsonText(text));
        return this;
    }

    @PetPetWhitelist
    public HostAPI actionbar(String text, boolean tinted) {
        if (isHost)
            MinecraftClient.getInstance().inGameHud.setOverlayMessage(DisplayUtils.tryParseJsonText(text), tinted);
        return this;
    }

    @Override
    public String toString() {
        return "HostAPI(isHost=" + isHost + ")";
    }
}
