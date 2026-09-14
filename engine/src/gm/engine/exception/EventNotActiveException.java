package gm.engine.exception;

import gm.dto.EventStatus;

/**
 * The requested action is legal only for an active event: one that its market maker has opened and
 * has not closed yet.
 */
public class EventNotActiveException extends GuessMarketException {

    private static final long serialVersionUID = 1L;

    public EventNotActiveException(int eventId, String eventName, EventStatus status,
                                   String marketMakerName) {
        super(status == EventStatus.INACTIVE
                ? "The event " + describeEvent(eventName, eventId) + " is not open yet, so it cannot be "
                        + "traded or closed. Its market maker [" + marketMakerName + "] has to open it "
                        + "first."
                : "The event " + describeEvent(eventName, eventId) + " is already closed, so it cannot "
                        + "be traded or closed again.");
    }
}
