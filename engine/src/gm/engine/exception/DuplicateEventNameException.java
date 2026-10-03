package gm.engine.exception;

/**
 * An event name is used more than once: by two events of the same file, or by an event that is
 * already in the system.
 */
public class DuplicateEventNameException extends GuessMarketException {

    private static final long serialVersionUID = 1L;

    public DuplicateEventNameException(String eventName) {
        super( "'" + eventName + "' not available",
                "The event name is already taken. Please choose another.");
    }
}
