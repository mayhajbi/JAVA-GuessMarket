package gm.engine.exception;

/**
 * An event name is used more than once: by two events of the same file, or by an event that is
 * already in the system.
 */
public class DuplicateEventNameException extends GuessMarketException {

    private static final long serialVersionUID = 1L;

    public DuplicateEventNameException(String eventName) {
        super("Event name taken", "An event named '" + eventName + "' already exists. Names are not case-sensitive. "
                + "Choose a different name.");
    }
}
