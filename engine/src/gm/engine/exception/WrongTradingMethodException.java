package gm.engine.exception;

import gm.dto.EventType;

/**
 * An action that belongs to one trading method was requested on an event of the other method (for
 * example, placing an order in an LMSR event).
 */
public class WrongTradingMethodException extends GuessMarketException {

    private static final long serialVersionUID = 1L;

    public WrongTradingMethodException(int eventId, String eventName, EventType type, String action) {
        super("The event " + describeEvent(eventName, eventId) + " uses the " + type.getDisplayName()
                + " method, so it is not possible to " + action + " in it.");
    }
}
