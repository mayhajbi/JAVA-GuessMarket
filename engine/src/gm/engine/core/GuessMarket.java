package gm.engine.core;

import gm.dto.CommissionType;
import gm.dto.EventStatus;
import gm.dto.EventType;
import gm.engine.exception.DuplicateEventIdException;
import gm.engine.exception.DuplicateUserNameException;
import gm.engine.exception.EventNotFoundException;
import gm.engine.exception.UserNotFoundException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The system itself: all the events that are currently loaded.
 * <p>
 * The events are kept in the order they appeared in the data file, so that the order the user sees
 * is stable between commands.
 */
public class GuessMarket implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Map<Integer, Event> eventsById = new LinkedHashMap<>();

    /** Keyed by the lower case name, so that the lookup is case insensitive. */
    private final Map<String, User> usersByName = new LinkedHashMap<>();

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

    /**
     * @return the event with this id, or {@code null} when no such event exists. Used while loading a
     *         file, where a missing id is reported with a message that fits its context.
     */
    public Event findEvent(int eventId) {
        return eventsById.get(eventId);
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

    /**
     * @return the events whose type, status and commission method are all among the given values, in
     *         the order of the data file
     */
    public List<Event> getEvents(Set<EventType> types, Set<EventStatus> statuses,
                                 Set<CommissionType> commissionTypes) {
        List<Event> matchingEvents = new ArrayList<>();
        for (Event event : eventsById.values()) {
            if (types.contains(event.getType()) && statuses.contains(event.getStatus())
                    && commissionTypes.contains(event.getCommissionType())) {
                matchingEvents.add(event);
            }
        }
        return matchingEvents;
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

    /**
     * Adds a user to the system.
     *
     * @throws DuplicateUserNameException when a user with the same name (ignoring case) already
     *                                    exists
     */
    public void addUser(User user) {
        String key = user.getName().toLowerCase(Locale.ROOT);
        if (usersByName.containsKey(key)) {
            throw new DuplicateUserNameException(user.getName());
        }
        usersByName.put(key, user);
    }

    /**
     * @throws UserNotFoundException when no user with this name (ignoring case) exists
     */
    public User getUser(String name) {
        String cleanName = name == null ? "" : name.trim();
        User user = usersByName.get(cleanName.toLowerCase(Locale.ROOT));
        if (user == null) {
            throw new UserNotFoundException(cleanName);
        }
        return user;
    }

    public Collection<User> getAllUsers() {
        return Collections.unmodifiableCollection(usersByName.values());
    }
}
