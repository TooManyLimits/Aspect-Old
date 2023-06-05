package io.github.moonlightmaya.script.apis;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.brigadier.StringReader;
import io.github.moonlightmaya.Aspect;
import io.github.moonlightmaya.util.DisplayUtils;
import io.github.moonlightmaya.util.EntityUtils;
import io.github.moonlightmaya.util.ItemUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.command.argument.ItemSlotArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import org.joml.Vector3d;
import petpet.external.PetPetWhitelist;
import petpet.lang.run.PetPetException;

@PetPetWhitelist
public class HostAPI {

    private final Aspect aspect;

    public boolean cursorUnlocked;

    public Vector3d chatColor = null;

    public HostAPI(Aspect aspect) {
        this.aspect = aspect;
    }

    @PetPetWhitelist
    public HostAPI sendChatMessage(String text) {
        ClientPlayNetworkHandler network = MinecraftClient.getInstance().getNetworkHandler();
        if (network != null) network.sendChatMessage(text);
        return this;
    }

    @PetPetWhitelist
    public HostAPI sendChatCommand(String text) {
        ClientPlayNetworkHandler network = MinecraftClient.getInstance().getNetworkHandler();
        if (network != null) network.sendChatCommand(text.startsWith("/") ? text.substring(1) : text);
        return this;
    }

    @PetPetWhitelist
    public HostAPI appendChatHistory(String text) {
        MinecraftClient.getInstance().inGameHud.getChatHud().addToMessageHistory(text);
        return this;
    }

    @PetPetWhitelist
    public HostAPI swingArm(boolean useOffHand) {
        if (MinecraftClient.getInstance().player != null)
            MinecraftClient.getInstance().player.swingHand(useOffHand ? Hand.OFF_HAND : Hand.MAIN_HAND);
        return this;
    }

    @PetPetWhitelist
    public ItemStack getSlot(Object arg) {
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
        this.chatColor = new Vector3d(r, g, b);
        return this;
    }

    @PetPetWhitelist
    public boolean isCursorUnlocked() {
        return cursorUnlocked;
    }

    @PetPetWhitelist
    public HostAPI unlockCursor() {
        cursorUnlocked = true;
        MinecraftClient.getInstance().mouse.unlockCursor();
        return this;
    }

    @PetPetWhitelist
    public HostAPI lockCursor() {
        cursorUnlocked = false;
        MinecraftClient.getInstance().mouse.lockCursor();
        return this;
    }

    @PetPetWhitelist
    public HostAPI titleTimes_1(Vector3d vec) {
        MinecraftClient.getInstance().inGameHud.setTitleTicks((int) vec.x, (int) vec.y, (int) vec.z);
        return this;
    }

    @PetPetWhitelist
    public HostAPI titleTimes_3(double x, double y, double z) {
        MinecraftClient.getInstance().inGameHud.setTitleTicks((int) x, (int) y, (int) z);
        return this;
    }

    @PetPetWhitelist
    public HostAPI title_0() {
        MinecraftClient.getInstance().inGameHud.clearTitle();
        return this;
    }

    @PetPetWhitelist
    public HostAPI title_1(String text) {
        MinecraftClient.getInstance().inGameHud.setTitle(DisplayUtils.tryParseJsonText(text));
        return this;
    }

    @PetPetWhitelist
    public HostAPI subtitle(String text) {
        MinecraftClient.getInstance().inGameHud.setSubtitle(DisplayUtils.tryParseJsonText(text));
        return this;
    }

    @PetPetWhitelist
    public HostAPI actionbar(String text, boolean tinted) {
        MinecraftClient.getInstance().inGameHud.setOverlayMessage(DisplayUtils.tryParseJsonText(text), tinted);
        return this;
    }

    //Funny renderings
    //host api because yeah

    @PetPetWhitelist
    public HostAPI enableScissors(double x, double y, double width, double height) {
        RenderSystem.enableScissor((int) x, (int) y, (int) width, (int) height);
        return this;
    }

    @PetPetWhitelist
    public HostAPI disableScissors() {
        RenderSystem.disableScissor();
        return this;
    }

    @PetPetWhitelist
    public HostAPI drawBuffers() {
        MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers().draw();
        return this;
    }

    //stringgg
    @Override
    public String toString() {
        return "HostAPI";
    }
}
