package gm.engine.exception;

/**
 * The selected option does not exist in the given event.
 */
public class InvalidOptionSelectionException extends GuessMarketException {

    private static final long serialVersionUID = 1L;

    public InvalidOptionSelectionException(String eventName, int optionCount, int requestedOptionNumber) {
        super(describeEvent(eventName) + " has no option number " + requestedOptionNumber
                + ". Choose an option from 1 to " + optionCount + ".");
    }
}
