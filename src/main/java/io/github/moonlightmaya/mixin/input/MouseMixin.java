package io.github.moonlightmaya.mixin.input;

import io.github.moonlightmaya.Aspect;
import io.github.moonlightmaya.manage.AspectManager;
import io.github.moonlightmaya.script.apis.HostAPI;
import io.github.moonlightmaya.script.events.EventHandler;
import io.github.moonlightmaya.util.EntityUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MouseMixin {

    @Shadow private double x;

    @Shadow private double y;

    @Shadow private boolean cursorLocked;

    @Inject(method = "onMouseButton", at = @At("HEAD"), cancellable = true)
    public void onClickEvent(long window, int button, int action, int mods, CallbackInfo ci) {
        if (window != MinecraftClient.getInstance().getWindow().getHandle())
            return;

        handleClickEventFor(AspectManager.getAspect(EntityUtils.getLocalUUID()), button, action, mods, ci);
        handleClickEventFor(AspectManager.getGuiAspect(), button, action, mods, ci);
    }
    private void handleClickEventFor(Aspect aspect, int button, int action, int mods, CallbackInfo ci) {
        if (aspect != null && aspect.scriptHandler != null) {
            String eventName = action == GLFW.GLFW_PRESS ? EventHandler.MOUSE_DOWN : EventHandler.MOUSE_UP;

            boolean wantsCancel = aspect.scriptHandler.callEventCancellable(eventName, x, y, button, mods);
            boolean inScreen = MinecraftClient.getInstance().currentScreen != null;

            if (wantsCancel && (!inScreen || cursorLocked))
                ci.cancel();

            HostAPI host = aspect.scriptHandler.getHostAPI();
            if (host != null && action == GLFW.GLFW_PRESS && host.cursorUnlocked && !inScreen)
                ci.cancel();
        }
    }

    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
    public void onScrollEvent(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (window != MinecraftClient.getInstance().getWindow().getHandle())
            return;
        handleScrollEventFor(AspectManager.getAspect(EntityUtils.getLocalUUID()), horizontal, vertical, ci);
        handleScrollEventFor(AspectManager.getGuiAspect(), horizontal, vertical, ci);
    }

    private void handleScrollEventFor(Aspect aspect, double horizontal, double vertical, CallbackInfo ci) {
        if (aspect != null && aspect.scriptHandler != null) {

            boolean wantsCancel = aspect.scriptHandler.callEventCancellable(EventHandler.MOUSE_SCROLL, vertical);
            boolean inScreen = MinecraftClient.getInstance().currentScreen != null;

            if (wantsCancel && (!inScreen || cursorLocked))
                ci.cancel();

            HostAPI host = aspect.scriptHandler.getHostAPI();
            if (host != null && host.cursorUnlocked && !inScreen)
                ci.cancel();
        }
    }

    @Inject(method = "onCursorPos", at = @At("HEAD"), cancellable = true)
    public void onMouseMove(long window, double x, double y, CallbackInfo ci) {
        if (window != MinecraftClient.getInstance().getWindow().getHandle())
            return;
        handleMouseMove(AspectManager.getAspect(EntityUtils.getLocalUUID()), x, y, ci);
        handleMouseMove(AspectManager.getGuiAspect(), x, y, ci);
    }

    private void handleMouseMove(Aspect aspect, double x, double y, CallbackInfo ci) {
        if (aspect != null && aspect.scriptHandler != null) {

            boolean wantsCancel = aspect.scriptHandler.callEventCancellable(EventHandler.MOUSE_MOVE, x, y);
            boolean inScreen = MinecraftClient.getInstance().currentScreen != null;

            if (wantsCancel && (!inScreen || cursorLocked)) {
                this.x = x;
                this.y = y;
                ci.cancel();
            }

            HostAPI host = aspect.scriptHandler.getHostAPI();
            if (host != null && host.cursorUnlocked && !inScreen)
                ci.cancel();
        }
    }

    @Inject(method = "lockCursor", at = @At("HEAD"), cancellable = true)
    public void onLockCursor(CallbackInfo ci) {
        handleLockCursorFor(AspectManager.getAspect(EntityUtils.getLocalUUID()), ci);
        handleLockCursorFor(AspectManager.getGuiAspect(), ci);
    }

    private void handleLockCursorFor(Aspect aspect, CallbackInfo ci) {
        if (aspect != null && aspect.scriptHandler != null) {
            HostAPI host = aspect.scriptHandler.getHostAPI();
            if (host != null && host.cursorUnlocked)
                ci.cancel();
        }
    }

}
