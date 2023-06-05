package io.github.moonlightmaya.script.events;

/**
 * An Event knows how many args it must be called with.
 * In this way, we can error when attempting to register
 * a function to an EventListener if the function's paramCount
 * and that of the Event are not equal.
 */
public class Event {
    public final String name;
    public final int paramCount;
    public final boolean piped;
    public Event(String name, int paramCount, boolean piped) {
        if (piped && paramCount != 1)
            throw new IllegalArgumentException("Piped events must have 1 arg");
        this.name = name;
        this.paramCount = paramCount;
        this.piped = piped;
    }
}