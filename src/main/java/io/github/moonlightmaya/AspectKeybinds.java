package io.github.moonlightmaya;

import io.github.moonlightmaya.manage.AspectManager;
import io.github.moonlightmaya.script.events.EventHandler;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.glfw.GLFW;

public class AspectKeybinds {

    private static final String CATEGORY = "key.category.aspect";

    public static final KeyBinding MENU;
    private static boolean menuOpen;

    static {
        MENU = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.aspect.menu", GLFW.GLFW_KEY_UNKNOWN, CATEGORY));

        //Menu
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (MENU.wasPressed()) {
                Aspect guiAspect = AspectManager.getGuiAspect();
                if (guiAspect != null && guiAspect.scriptHandler != null) {
                    menuOpen = !menuOpen;
                    guiAspect.scriptHandler.callEvent(menuOpen ? EventHandler.GUI_OPEN : EventHandler.GUI_CLOSE);
                }
            }
        });
    }

    public static void registerKeybinds() {
        //literally just load the class, activating static stuff
    }

}
