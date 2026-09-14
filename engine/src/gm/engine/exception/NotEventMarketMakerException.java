package gm.engine.exception;

/**
 * A user tried to perform an action that only the market maker of the event may perform (opening or
 * closing it).
 */
public class NotEventMarketMakerException extends GuessMarketException {

    private static final long serialVersionUID = 1L;

    public NotEventMarketMakerException(int eventId, String eventName, String marketMakerName,
                                        String userName, String action) {
        super("The user [" + userName + "] cannot " + action + " the event "
                + describeEvent(eventName, eventId) + ". Only its market maker [" + marketMakerName
                + "] may do that.");
    }
}
