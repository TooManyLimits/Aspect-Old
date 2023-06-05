package io.github.moonlightmaya.mixin.gui;

import io.github.moonlightmaya.Aspect;
import io.github.moonlightmaya.manage.AspectManager;
import io.github.moonlightmaya.script.apis.HostAPI;
import io.github.moonlightmaya.script.events.Events;
import io.github.moonlightmaya.util.ColorUtils;
import io.github.moonlightmaya.util.EntityUtils;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatScreen.class)
public class ChatScreenMixin {

    @Shadow protected TextFieldWidget chatField;

    @ModifyVariable(at = @At("HEAD"), method = "sendMessage", argsOnly = true)
    private String onMessage(String message) {
        //Potentially modify the sent message
        Aspect localAspect = AspectManager.getAspect(EntityUtils.getLocalUUID());
        if (localAspect != null && !message.isBlank()) {
            Object result = localAspect.script.callEvent(Events.SEND_CHAT_MESSAGE, message);
            message = localAspect.script.getStringFor(result);
        }
        return message;
    }

    @Inject(at = @At("HEAD"), method = "render")
    public void changeChatColor(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        handleChatColorSetting(AspectManager.getGuiAspect());
        handleChatColorSetting(AspectManager.getAspect(EntityUtils.getLocalUUID()));
    }

    private void handleChatColorSetting(Aspect aspect) {
        if (aspect != null && aspect.script != null) {
            HostAPI api = aspect.script.getHostAPI();
            if (api != null) {
                Vector3d rgb = api.chatColor;
                if (rgb != null)
                    this.chatField.setEditableColor(ColorUtils.VecToIntARGB(rgb));
            }
        }
    }

}
