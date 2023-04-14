package io.github.moonlightmaya.script.events;

import org.apache.commons.lang3.mutable.MutableBoolean;
import petpet.external.PetPetWhitelist;
import petpet.lang.run.PetPetCallable;
import petpet.lang.run.PetPetException;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@PetPetWhitelist
public class AspectEvent {

    /**
     * Maintains the list of all registered functions to the event
     */
    private final Set<PetPetCallable> registered = new LinkedHashSet<>();

    private boolean currentlyRunning;
    private final String name;
    private final int expectedArgCount;

    public AspectEvent(String name, int expectedArgCount) {
        this.name = name;
        this.expectedArgCount = expectedArgCount;
    }

    /**
     * Registers the given function to the event
     * Adding the same function twice will error
     */
    @PetPetWhitelist
    public void add(PetPetCallable function) {
        if (expectedArgCount != function.paramCount())
            throw new PetPetException("Event " + name + " expects a " + expectedArgCount + "-arg function, but received a " + function.paramCount() + "-arg function");
        if (currentlyRunning)
            throw new PetPetException("Cannot add to an event while inside that event");
        if (!registered.add(function))
            throw new PetPetException("Attempt to register the same function to an event multiple times");
    }

    /**
     * Removes the function from the event if it exists
     * Returns true if the given function was in the event,
     * false if it wasn't inside the event
     */
    @PetPetWhitelist
    public boolean remove(PetPetCallable function) {
        if (currentlyRunning)
            throw new PetPetException("Cannot remove from an event while inside that event!");
        return registered.remove(function);
    }

    /**
     * Executes the event.
     * The boolean flag ensures that the list cannot be
     * modified while it's being iterated.
     * The given args will be passed to each function in the call.
     *
     * Return value is the result of the final function call, or null if none
     */
    public Object execute(Object... args) {
        currentlyRunning = true;
        Object o = null;
        for (PetPetCallable func : registered)
            o = func.call(args);
        currentlyRunning = false;
        return o;
    }

    @Override
    public String toString() {
        return "Event(" + registered.size() + " functions)";
    }
}