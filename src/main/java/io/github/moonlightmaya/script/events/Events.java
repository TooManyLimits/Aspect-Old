package io.github.moonlightmaya.script.events;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Maintains information about the set of all Event objects.
 */
public class Events {

    private static final Map<String, Event> ALL_EVENTS = new HashMap<>();

    /**
     * Creates the event
     * @param name
     * @param event
     */
    public static Event registerEvent(String name, Event event) {
        if (!Objects.equals(name, event.name))
            throw new IllegalArgumentException("Event name field and argument don't match: Field is \"" + event.name + "\", arg is \"" + name + "\"");
        if (!name.matches("[A-Z_]+"))
            throw new IllegalArgumentException("Event names are expected to be SNAKE_CASE, got \"" + name + "\"");
        ALL_EVENTS.put(name, event);
        return event;
    }

    /**
     * Creates the event for you with the given name and arg count, then registers it
     * The event is not assumed to be piped
     */
    public static Event registerEvent(String name, int args) {
        return registerEvent(name, new Event(name, args, false));
    }

    public static Event registerPipedEvent(String name) {
        return registerEvent(name, new Event(name, 1, true));
    }

    public static @Nullable Event getEventByName(String name) {
        return ALL_EVENTS.get(name);
    }

    /**
     * All the built-in Aspect events are below.
     */

    public static final Event WORLD_TICK = registerEvent("WORLD_TICK", 0);
    public static final Event TICK = registerEvent("TICK", 0);

    public static final Event WORLD_RENDER = registerEvent("WORLD_RENDER", 2);
    public static final Event POST_WORLD_RENDER = registerEvent("POST_WORLD_RENDER", 2);
    public static final Event RENDER = registerEvent("RENDER", 2);
    public static final Event POST_RENDER = registerEvent("POST_RENDER", 2);
    public static final Event HUD_RENDER = registerEvent("HUD_RENDER", 1);
    public static final Event POST_HUD_RENDER = registerEvent("POST_HUD_RENDER", 1);

    public static final Event USER_INIT = registerEvent("USER_INIT", 0);
    public static final Event USER_LOAD = registerEvent("USER_LOAD", 0);
    public static final Event USER_UNLOAD = registerEvent("USER_UNLOAD", 0);

    public static final Event WORLD_CHANGE = registerEvent("WORLD_CHANGE", 0);

    public static final Event SEND_CHAT_MESSAGE = registerPipedEvent("SEND_CHAT_MESSAGE");
    public static final Event RECEIVE_CHAT_MESSAGE = registerEvent("RECEIVE_CHAT_MESSAGE", 1);

    public static final Event MOUSE_DOWN = registerEvent("MOUSE_DOWN", 4);
    public static final Event MOUSE_UP = registerEvent("MOUSE_UP", 4);
    public static final Event MOUSE_MOVE = registerEvent("MOUSE_MOVE", 2);
    public static final Event MOUSE_SCROLL = registerEvent("MOUSE_SCROLL", 1);

    public static final Event GUI_OPEN = registerEvent("GUI_OPEN", 0);
    public static final Event GUI_CLOSE = registerEvent("GUI_CLOSE", 0);







}
