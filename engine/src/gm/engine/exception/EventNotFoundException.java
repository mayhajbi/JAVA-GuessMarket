package gm.engine.exception;

/**
 * The requested event does not exist in the system.
 */
public class EventNotFoundException extends GuessMarketException {

    private static final long serialVersionUID = 1L;

    public EventNotFoundException(int eventId) {
        super("There is no event with id " + eventId + " in the system.");
    }
}
