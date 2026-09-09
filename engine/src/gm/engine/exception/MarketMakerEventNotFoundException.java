package gm.engine.exception;

/**
 * A user is declared as the market maker of an event id that no event in the file uses.
 */
public class MarketMakerEventNotFoundException extends GuessMarketException {

    private static final long serialVersionUID = 1L;

    public MarketMakerEventNotFoundException(String userName, int eventId) {
        super("The user [" + userName + "] is defined as the market maker of event id " + eventId
                + ", but no event with that id exists in the file.");
    }
}
