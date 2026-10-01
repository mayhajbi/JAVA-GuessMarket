package gm.engine.exception;

/**
 * The selected option does not exist in the given event.
 */
public class InvalidOptionSelectionException extends GuessMarketException {

    private static final long serialVersionUID = 1L;

    public InvalidOptionSelectionException(String eventName, int optionCount, int requestedOptionNumber) {
        super("The option number " + requestedOptionNumber + " does not exist in the event "
                + describeEvent(eventName) + ". Please choose a number between 1 and " + optionCount + ".");
    }
}
