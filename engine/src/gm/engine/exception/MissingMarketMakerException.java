package gm.engine.exception;

/**
 * No user in the data file is declared as the market maker of an event.
 */
public class MissingMarketMakerException extends GuessMarketException {

    private static final long serialVersionUID = 1L;

    public MissingMarketMakerException(int eventId, String eventName) {
        super("The event [" + eventName + "] (id " + eventId + ") has no market maker. "
                + "Exactly one user in the file must be defined as its market maker.");
    }
}
