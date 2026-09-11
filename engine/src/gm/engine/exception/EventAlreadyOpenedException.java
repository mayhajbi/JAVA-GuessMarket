package gm.engine.exception;

import gm.dto.EventStatus;

/**
 * An event that was already opened (it is active or closed) was asked to open again.
 */
public class EventAlreadyOpenedException extends GuessMarketException {

    private static final long serialVersionUID = 1L;

    public EventAlreadyOpenedException(int eventId, String eventName, EventStatus status) {
        super("The event [" + eventName + "] (id " + eventId + ") cannot be opened: it is already "
                + status.getDisplayName().toLowerCase() + ". An event can be opened only once.");
    }
}
