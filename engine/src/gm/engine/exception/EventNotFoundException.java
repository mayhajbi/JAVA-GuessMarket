package gm.engine.exception;

/**
 * The requested event does not exist in the system.
 */
public class EventNotFoundException extends GuessMarketException {

    private static final long serialVersionUID = 1L;

    public EventNotFoundException(int eventId) {
        super("Event not found", "Event " + eventId + " was not found. Select an event from the list.");
    }
}
