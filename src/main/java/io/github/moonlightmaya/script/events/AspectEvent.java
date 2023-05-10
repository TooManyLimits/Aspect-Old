package io.github.moonlightmaya.script.events;

import petpet.external.PetPetWhitelist;
import petpet.lang.run.PetPetCallable;
import petpet.lang.run.PetPetException;

import java.util.LinkedHashSet;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

@PetPetWhitelist
public class AspectEvent {

    /**
     * Maintains the list of all registered functions to the event
     */
    private final LinkedHashSet<PetPetCallable> registered = new LinkedHashSet<>();
    private final ConcurrentLinkedQueue<Consumer<LinkedHashSet<PetPetCallable>>> queuedChanges = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<PetPetCallable> toRegister = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<PetPetCallable> toRemove = new ConcurrentLinkedQueue<>();
    private boolean shouldClear = false;
    private final String name;
    private final int expectedArgCount;

    public AspectEvent(String name, int expectedArgCount) {
        this.name = name;
        this.expectedArgCount = expectedArgCount;
    }

    private void flushQueues() {
        if (shouldClear) {
            queuedChanges.clear();
            registered.clear();
            toRegister.clear();
            toRemove.clear();
            shouldClear = false;
            return;
        }
        while (!queuedChanges.isEmpty())
            queuedChanges.poll().accept(registered);
    }

    @PetPetWhitelist
    public void clear() {
        shouldClear = true;
    }

    /**
     * Registers the given function to the event
     * Registering the same function multiple times is the same
     * as only registering it once
     */
    @PetPetWhitelist
    public void add(PetPetCallable function) {
        if (expectedArgCount != function.paramCount())
            throw new PetPetException("Event " + name + " expects a " + expectedArgCount + "-arg function, but received a " + function.paramCount() + "-arg function");
        queuedChanges.add(c -> c.add(function));
    }

    /**
     * Removes the function from the event if it exists
     */
    @PetPetWhitelist
    public void remove(PetPetCallable function) {
        queuedChanges.add(c -> c.remove(function));
    }

    /**
     * Executes the event.
     * The set of registered functions cannot be modified while
     * inside the iteration.
     * The given args will be passed to each function in the call.
     */
    public void execute(Object... args) {
        flushQueues();
        for (PetPetCallable func : registered)
            func.call(args);
    }

    /**
     * Executes the function in a "piped" manner.
     * The initial object is passed in to the
     * first function, then the output of this
     * function is passed to the next, and so on.
     * Eventually, the final object after passing
     * through all registered functions is returned.
     */
    public Object executePiped(Object arg) {
        flushQueues();
        for (PetPetCallable func : registered)
            arg = func.call(arg);
        return arg;
    }

    /**
     * Executes the function in a "piped" manner.
     * The initial object is passed in to the
     * first function, then the output of this
     * function is passed to the next, and so on.
     * Eventually, the final object after passing
     * through all registered functions is returned.
     */
    public boolean executeCancellable(Object... args) {
        flushQueues();
        boolean cancelled = false;
        for (PetPetCallable func : registered) {
            Object result = func.call(args);
            if (result instanceof Boolean b && b)
                cancelled = true;
        }
        return cancelled;
    }

    @Override
    public String toString() {
        return "Event(" + registered.size() + " functions)";
    }
}
