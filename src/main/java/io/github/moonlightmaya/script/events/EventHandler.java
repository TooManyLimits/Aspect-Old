package io.github.moonlightmaya.script.events;

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

    private Map<String, AspectEvent> events = new HashMap<>();

    /**
     * Creates the event handler and installs it into the given instance
     */
    public EventHandler(PetPetInstance instance) {
        PetPetTable table = new PetPetTable();
        for (String name : EVENT_NAMES) {
            AspectEvent event = new AspectEvent();
            events.put(name, event);
            table.put(name, event);
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
    private static final Set<String> EVENT_NAMES = new HashSet<>();
    public static void defineEvent(String name) {
        EVENT_NAMES.add(name);
    }

    static {
        defineEvent("tick");
        defineEvent("render");
    }


}
