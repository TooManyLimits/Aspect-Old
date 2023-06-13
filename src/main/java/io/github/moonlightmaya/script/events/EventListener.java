package io.github.moonlightmaya.script.events;

import org.jetbrains.annotations.NotNull;
import petpet.external.PetPetWhitelist;
import petpet.lang.run.PetPetCallable;
import petpet.lang.run.PetPetClosure;
import petpet.lang.run.PetPetException;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Queue;

@PetPetWhitelist
public class EventListener implements Iterable<PetPetCallable> {

    private final Event event;

    private final LinkedHashSet<PetPetCallable> registered = new LinkedHashSet<>();

    public EventListener(Event event) {
        this.event = event;
    }

    /**
     * Maintain these two queues of addition and removal. Since user-supplied
     * functions are called while iterating over the set, it's possible
     * that new functions will be added to the event as we iterate. This would
     * throw a ConcurrentModificationException if it happened, so instead we
     * collect the values in these queues, and flush the queues whenever an
     * iterator is requested.
     */
    private final Queue<PetPetCallable> queuedForAdd = new LinkedList<>();
    private final Queue<PetPetCallable> queuedForDel = new LinkedList<>();

    @NotNull
    @Override
    public Iterator<PetPetCallable> iterator() {
        //Flush the queues and return an iterator.
        while (!queuedForAdd.isEmpty())
            registered.add(queuedForAdd.poll());
        while (!queuedForDel.isEmpty())
            registered.remove(queuedForDel.poll());
        return registered.iterator();
    }

    /**
     * PetPet Functions below
     */

    /**
     * Add this function to the event listener.
     * It will be called each time the event listener activates.
     * Returns the function you passed in.
     */
    @PetPetWhitelist
    public PetPetCallable add(PetPetCallable callable) {
        if (callable.paramCount() != event.paramCount)
            throw new PetPetException("Event " + event.name + " expects " + event.paramCount + " args, but provided function has only " + callable.paramCount());
        if (!(callable instanceof PetPetClosure))
            throw new PetPetException("Only PetPet functions may be added to events. Java functions cannot!");
        queuedForAdd.add(callable);
        return callable;
    }

    /**
     * Deletes the given function from the EventListener.
     * It returns true if the EventListener contained the function,
     * if it did not contain the function then it returns false.
     */
    @PetPetWhitelist
    public void del(PetPetCallable callable) {
        queuedForDel.add(callable);
    }

    /**
     * Return the number of functions registered to this
     * EventListener
     */
    @PetPetWhitelist
    public double count() {
        return registered.size();
    }


}
