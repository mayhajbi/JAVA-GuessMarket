package gm.engine.core;

import gm.dto.CommissionType;
import gm.dto.EventStatus;
import gm.dto.EventType;
import gm.engine.exception.DuplicateEventNameException;
import gm.engine.exception.DuplicateUserNameException;
import gm.engine.exception.UserNotFoundException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The system itself: all its events and users.
 * <p>
 * The events are kept in the order they were added, so that the order the user sees is stable.
 */
public class GuessMarket {

    private final Map<Integer, Event> eventsById = new LinkedHashMap<>();

    /** Keyed by the lower case name, so that a name that is already in use is found without a scan. */
    private final Map<String, Event> eventsByName = new HashMap<>();

    /** Keyed by the lower case name, so that the lookup is case insensitive. */
    private final Map<String, User> usersByName = new LinkedHashMap<>();

    /**
     * Adds an event to the system. Its id is one the system gave it ({@link #nextEventId()}).
     *
     * @throws DuplicateEventNameException when an event with the same name (ignoring case) already
     *                                     exists
     */
    public void addEvent(Event event) {
        requireNameNotInUse(event.getName());
        eventsById.put(event.getId(), event);
        eventsByName.put(nameKey(event.getName()), event);
    }

    /**
     * Adds all the events of a file, or none of them: the names are checked against
     * the system and against each other before the first event is added, and the events get the next
     * ids of the system, in the order of the list.
     *
     * @throws DuplicateEventNameException when a name is already in use, or appears twice in the list
     */
    public void addEvents(List<Event> events) {
        Set<String> namesInList = new HashSet<>();
        for (Event event : events) {
            requireNameNotInUse(event.getName());
            if (!namesInList.add(nameKey(event.getName()))) {
                throw new DuplicateEventNameException(event.getName());
            }
        }
        int nextId = nextEventId();
        for (Event event : events) {
            event.assignId(nextId++);
            addEvent(event);
        }
    }

    private void requireNameNotInUse(String name) {
        if (eventsByName.containsKey(nameKey(name))) {
            throw new DuplicateEventNameException(name);
        }
    }

    private static String nameKey(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    /**
     * @return an id no event in the system uses, for a new event
     */
    public int nextEventId() {
        int highestId = 0;
        for (int id : eventsById.keySet()) {
            highestId = Math.max(highestId, id);
        }
        return highestId + 1;
    }

    /**
     * @throws IllegalArgumentException when no event with this id exists
     */
    public Event getEvent(int eventId) {
        Event event = eventsById.get(eventId);
        if (event == null) {
            throw new IllegalArgumentException("No event with id " + eventId);
        }
        return event;
    }

    public Collection<Event> getAllEvents() {
        return Collections.unmodifiableCollection(eventsById.values());
    }

    /**
     * @return the events whose type, status and commission method are all among the given values, in
     *         the order they were added
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
