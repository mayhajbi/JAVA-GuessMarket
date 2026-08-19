package gm.engine.exception;

/**
 * The requested action is legal only for an event that is still active.
 */
public class EventNotActiveException extends GuessMarketException {

    private static final long serialVersionUID = 1L;

    public EventNotActiveException(int eventId, String eventName) {
        super("The event [" + eventName + "] (id " + eventId + ") is already closed, so it cannot "
                + "be traded or closed again.");
    }
}
