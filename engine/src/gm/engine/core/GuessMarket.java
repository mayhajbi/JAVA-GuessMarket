package gm.engine.core;

import gm.engine.exception.DuplicateEventIdException;
import gm.engine.exception.EventNotFoundException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The system itself: all the events that are currently loaded.
 * <p>
 * The events are kept in the order they appeared in the data file, so that the order the user sees
 * is stable between commands.
 */
public class GuessMarket implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Map<Integer, Event> eventsById = new LinkedHashMap<>();

    /**
     * Adds an event to the system.
     *
     * @throws DuplicateEventIdException when an event with the same id already exists
     */
    public void addEvent(Event event) {
        Event existingEvent = eventsById.get(event.getId());
        if (existingEvent != null) {
            throw new DuplicateEventIdException(event.getId(), existingEvent.getName(),
                    event.getName());
        }
        eventsById.put(event.getId(), event);
    }

    /**
     * @throws EventNotFoundException when no event with this id exists
     */
    public Event getEvent(int eventId) {
        Event event = eventsById.get(eventId);
        if (event == null) {
            throw new EventNotFoundException(eventId);
        }
        return event;
    }

    public Collection<Event> getAllEvents() {
        return Collections.unmodifiableCollection(eventsById.values());
    }

    public List<Event> getActiveEvents() {
        List<Event> activeEvents = new ArrayList<>();
        for (Event event : eventsById.values()) {
            if (event.isActive()) {
                activeEvents.add(event);
            }
        }
        return activeEvents;
    }

    public int getEventCount() {
        return eventsById.size();
    }

    public double getTotalSubsidy() {
        double total = 0;
        for (Event event : eventsById.values()) {
            total += event.getInitialSubsidy();
        }
        return total;
    }
}
