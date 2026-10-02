package gm.engine.exception;

import gm.dto.EventStatus;

/**
 * The requested action is legal only for an active event: one that its market maker has opened and
 * has not closed yet.
 */
public class EventNotActiveException extends GuessMarketException {

    private static final long serialVersionUID = 1L;

    public EventNotActiveException(String eventName, EventStatus status,
                                   String marketMakerName) {
        super(status == EventStatus.INACTIVE ? "Event not open" : "Event closed",
                status == EventStatus.INACTIVE
                ? describeEvent(eventName) + " is not open yet. Only its market maker, '" + marketMakerName
                        + "', can open it."
                : describeEvent(eventName) + " is closed, so it can no longer be traded or closed.");
    }
}
