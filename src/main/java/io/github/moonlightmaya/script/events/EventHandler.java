package io.github.moonlightmaya.script.events;

import oshi.util.tuples.Pair;
import petpet.external.PetPetInstance;
import petpet.types.PetPetTable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Deals with the global "events" table and the events that it contains
 */
public class EventHandler {

    private final Map<String, AspectEvent> events = new HashMap<>();

    /**
     * Creates the event handler and installs it into the given instance
     */
    public EventHandler(PetPetInstance instance) {
        PetPetTable<String, AspectEvent> table = new PetPetTable<>();
        for (Pair<String, Integer> type : EVENT_TYPES) {
            AspectEvent event = new AspectEvent(type.getA(), type.getB());
            events.put(type.getA(), event); //build the java-side and petpet-side mappings
            table.put(type.getA(), event);
        }
        instance.setGlobal("events", table);
    }

    /**
     * Calls the given event with the given args.
     * The result is the result of the final function
     * registered, or null if none are registered.
     */
    public void callEvent(String name, Object... args) {
        events.get(name).execute(args);
    }

    /**
     * Calls the event in the "piped" style, with the
     * given arg. The result of a call is passed to
     * the next call.
     */
    public Object callEventPiped(String name, Object arg) {
        return events.get(name).executePiped(arg);
    }

    /**
     * Defines a new event, which will then be added to every Aspect's
     * events table upon creation.
     *
     * Public in case other mods later want to add their own new events,
     * and also is not an enum, for the same reason. Enums are hard for
     * other mods to extend, and add their own events.
     */
    private static final Set<Pair<String, Integer>> EVENT_TYPES = new HashSet<>();
    public static void defineEvent(String name, int argCount) {
        EVENT_TYPES.add(new Pair<>(name, argCount));
    }

    public static final String
            WORLD_TICK = "world_tick",
            TICK = "tick",
            WORLD_RENDER = "world_render",
            RENDER = "render",
            USER_INIT = "user_init",
            USER_LOAD = "user_load",
            USER_UNLOAD = "user_unload",
            WORLD_CHANGE = "world_change";


    static {
        defineEvent(WORLD_TICK, 0);
        defineEvent(TICK, 0);

        defineEvent(WORLD_RENDER, 1);
        defineEvent(RENDER, 1);

        defineEvent(USER_INIT, 0);
        defineEvent(USER_LOAD, 0);
        defineEvent(USER_UNLOAD, 0);

        defineEvent(WORLD_CHANGE, 0);
    }


}
