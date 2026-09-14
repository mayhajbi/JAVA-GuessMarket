package gm.engine.exception;

/**
 * More than one user in the data file is declared as the market maker of the same event.
 */
public class MultipleMarketMakersException extends GuessMarketException {

    private static final long serialVersionUID = 1L;

    public MultipleMarketMakersException(int eventId, String eventName, String firstUser,
                                         String secondUser) {
        super("The event " + describeEvent(eventName, eventId) + " has more than one market maker: ["
                + firstUser + "] and [" + secondUser + "]. Exactly one user must be defined as its "
                + "market maker.");
    }
}
