package gm.engine.exception;

/**
 * A user tried to perform an action that only the market maker of the event may perform (opening or
 * closing it).
 */
public class NotEventMarketMakerException extends GuessMarketException {

    private static final long serialVersionUID = 1L;

    public NotEventMarketMakerException(String eventName, String marketMakerName,
                                        String action) {
        super("You cannot " + action + " " + describeEvent(eventName) + ". Only its market maker, '"
                + marketMakerName + "', can do that.");
    }
}
