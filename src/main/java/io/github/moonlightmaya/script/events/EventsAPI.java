package io.github.moonlightmaya.script.events;

import petpet.external.PetPetWhitelist;
import petpet.lang.run.PetPetCallable;
import petpet.lang.run.PetPetException;
import petpet.types.PetPetTable;

import java.util.Map;

/**
 * An "Event" is something that happens on occasion.
 * There are a fixed number of Events, defined by Aspect
 * or other mods.
 * An "EventListener" is an object inside a script that,
 * when a certain event happens, is activated.
 * An "EventsAPI" is an object inside a script that controls
 * and manages several EventListener objects.
 *
 * An EventsAPI handles events for a given Aspect
 * It can be indexed to retrieve the corresponding
 * EventListener object for a String name
 */
@PetPetWhitelist
public class EventsAPI {

    private final Map<String, EventListener> listeners = new PetPetTable<>();

    /**
     * Runs the given event. The return value of the event
     * is whatever the return value of the final function was,
     * or null if no functions were called.
     */
    public Object callEvent(Event event, Object... args) {
        if (event.paramCount != args.length)
            throw new RuntimeException("Attempt to call event java-side with wrong number of args! Error with the mod, not with your code - contact devs");
        //Get the listener
        EventListener listener = listeners.get(event.name);

        if (event.piped) {
            Object arg = args[0];
            //If it doesn't exist, returns the arg directly
            if (listener != null)
                for (PetPetCallable func : listener)
                    arg = func.call(arg);
            return arg;
        } else {
            //If it doesn't exist, returns null directly
            if (listener == null)
                return null;
            Object result = null;
            for (PetPetCallable func : listener)
                result = func.call(args);
            return result;
        }
    }

    /**
     * An EventsAPI contains several event listeners inside.
     * Get one like this by just indexing it:
     */
    @PetPetWhitelist
    public EventListener __get_str(String key) {
        //Get the listener corresponding to the given name, or else create one
        return listeners.computeIfAbsent(key.toUpperCase(), s -> {
            Event event = Events.getEventByName(s.toUpperCase());
            if (event == null)
                throw new PetPetException("Unrecognized event name \"" + s + "\"");
            return new EventListener(event);
        });
    }

    /**
     * Allow the syntax sugar of
     * fn events.tick() {
     *     //...
     * }
     */
    @PetPetWhitelist
    public PetPetCallable __set_str(String key, PetPetCallable value) {
        EventListener listener = __get_str(key);
        listener.add(value);
        return value;
    }

}
