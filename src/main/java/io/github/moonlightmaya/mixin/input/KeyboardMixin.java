package io.github.moonlightmaya.mixin.input;

import io.github.moonlightmaya.Aspect;
import io.github.moonlightmaya.game_interfaces.AspectKeybinds;
import io.github.moonlightmaya.manage.AspectManager;
import io.github.moonlightmaya.script.events.Events;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public class KeyboardMixin {


    @Shadow @Final private MinecraftClient client;

    /**
     * If the escape key gets pressed, and we're inside a GUI aspect, then we want to
     * close the GUI in the same way that regular Minecraft screens are closed.
     */

    @Inject(method = "onKey", at = @At("HEAD"), cancellable = true)
    public void interceptEscapeKeyForAspectGui(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
        if (window != client.getWindow().getHandle()) return;

        if (key == GLFW.GLFW_KEY_ESCAPE && client.currentScreen == null && AspectKeybinds.menuOpen) {
            Aspect guiAspect = AspectManager.getGuiAspect();
            if (guiAspect != null && guiAspect.script != null) {
                AspectKeybinds.menuOpen = false;
                guiAspect.script.callEvent(Events.GUI_CLOSE);
            }
            ci.cancel();
        }
    }


}
