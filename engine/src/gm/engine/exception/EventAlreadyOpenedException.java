package gm.engine.exception;

import gm.dto.EventStatus;

import java.util.Locale;

/**
 * An event that was already opened (it is active or closed) was asked to open again.
 */
public class EventAlreadyOpenedException extends GuessMarketException {

    private static final long serialVersionUID = 1L;

    public EventAlreadyOpenedException(String eventName, EventStatus status) {
        super("Event already open", describeEvent(eventName) + " is already " + status.getDisplayName().toLowerCase(Locale.ROOT)
                + ". An event can be opened only once.");
    }
}
