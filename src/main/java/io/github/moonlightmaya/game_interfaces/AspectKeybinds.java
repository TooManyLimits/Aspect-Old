package io.github.moonlightmaya.game_interfaces;

import io.github.moonlightmaya.Aspect;
import io.github.moonlightmaya.manage.AspectManager;
import io.github.moonlightmaya.script.events.Events;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.glfw.GLFW;

/**
 * Registers keybinds for Aspect
 */
public class AspectKeybinds {

    private static final String CATEGORY = "key.category.aspect";

    public static final KeyBinding MENU;
    public static boolean menuOpen;

    static {
        MENU = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.aspect.menu", GLFW.GLFW_KEY_UNKNOWN, CATEGORY));

        //Menu
        //NOTE: Menu is also managed by KeyboardMixin.java! (Handling the ESC key to close)
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (MENU.wasPressed()) {
                Aspect guiAspect = AspectManager.getGuiAspect();
                if (guiAspect != null && guiAspect.script != null) {
                    menuOpen = !menuOpen;
                    guiAspect.script.callEvent(menuOpen ? Events.GUI_OPEN : Events.GUI_CLOSE);
                }
            }
        });
    }

    //literally just load the class, activating static stuff
    public static void registerKeybinds() {}

}
