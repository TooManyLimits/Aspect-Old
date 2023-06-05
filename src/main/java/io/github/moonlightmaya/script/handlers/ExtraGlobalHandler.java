package io.github.moonlightmaya.script.handlers;

import io.github.moonlightmaya.script.AspectScript;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Handles setting up extra global state for an AspectScriptHandler.
 * This also works
 */
public class ExtraGlobalHandler {

    /**
     * A list of things which will affect the global state somehow.
     * Mods which want to do things should add to this.
     */
    private static final List<Consumer<AspectScript>> REGISTERED = new ArrayList<>();

    public static void setupInstance(AspectScript scriptHandler) {
        for (Consumer<AspectScript> handler : REGISTERED)
            handler.accept(scriptHandler);
    }

    /**
     * Any mods who want to edit globals should call this!
     */
    public static void register(Consumer<AspectScript> consumer) {
        REGISTERED.add(consumer);
    }

    /**
     * If you want some petpet code to be run at startup, like the
     * util files in Aspect's script resources
     */
    public static void registerUtil(String name, String sourceCode) {
        REGISTERED.add(handler -> {
            try {
                handler.runCode(name, sourceCode);
            } catch (Throwable t) {
                handler.error(t);
            }
        });
    }

}
