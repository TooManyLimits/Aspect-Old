package io.github.moonlightmaya.script.events;

import io.github.moonlightmaya.script.AspectScriptHandler;
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
            events.put(type.getA(), event);
            table.put(type.getA(), event);
        }
        instance.setGlobal("events", table);
    }

    /**
     * Calls the given event with the given args.
     * The result is the result of the final function
     * registered, or null if none are registered.
     */
    public Object callEvent(String name, Object... args) {
        return events.get(name).execute(args);
    }

    /**
     * Defines a new event, which will then be added to every Aspect's
     * events table upon creation.
     *
     * Public in case other mods later want to add their own new events.
     */
    private static final Set<Pair<String, Integer>> EVENT_TYPES = new HashSet<>();
    public static void defineEvent(String name, int argCount) {
        EVENT_TYPES.add(new Pair<>(name, argCount));
    }

    static {
        defineEvent("world_tick", 0);
        defineEvent("tick", 0);
        defineEvent("world_render", 1);
        defineEvent("render", 1);
    }


}
