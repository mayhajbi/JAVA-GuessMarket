package gm.engine.exception;

import gm.dto.EventStatus;

import java.util.Locale;

/**
 * An event that was already opened (it is active or closed) was asked to open again.
 */
public class EventAlreadyOpenedException extends GuessMarketException {

    private static final long serialVersionUID = 1L;

    public EventAlreadyOpenedException(String eventName) {
        super("Event already open", describeEvent(eventName) + " was already opened, and cannot be opened again.");
    }
}
