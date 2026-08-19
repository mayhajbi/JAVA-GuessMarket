package gm.engine.exception;

/**
 * The selected option does not exist in the given event.
 */
public class InvalidOptionSelectionException extends GuessMarketException {

    private static final long serialVersionUID = 1L;

    public InvalidOptionSelectionException(int eventId, int optionCount, int requestedOptionNumber) {
        super("The option number " + requestedOptionNumber + " does not exist in the event with id "
                + eventId + ". Please choose a number between 1 and " + optionCount + ".");
    }
}
