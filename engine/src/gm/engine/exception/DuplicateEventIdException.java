package gm.engine.exception;

/**
 * Two events in the data file share the same id.
 */
public class DuplicateEventIdException extends GuessMarketException {

    private static final long serialVersionUID = 1L;

    public DuplicateEventIdException(int id, String firstEventName, String secondEventName) {
        super("The event id " + id + " appears more than once in the file: it is used both by the "
                + "event [" + firstEventName + "] and by the event [" + secondEventName + "]. "
                + "Every event must have a unique id.");
    }
}
