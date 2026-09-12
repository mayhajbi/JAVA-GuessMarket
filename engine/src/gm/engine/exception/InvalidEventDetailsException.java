package gm.engine.exception;

/**
 * The details a user filled in for a new event are not enough to create it (bonus). A data file
 * reports the same faults with a message that points at the element that is missing.
 */
public class InvalidEventDetailsException extends GuessMarketException {

    private static final long serialVersionUID = 1L;

    private InvalidEventDetailsException(String message) {
        super(message);
    }

    public static InvalidEventDetailsException missingName() {
        return new InvalidEventDetailsException("The new event has no name. "
                + "Please give the event a name.");
    }

    public static InvalidEventDetailsException missingDescription(String eventName) {
        return new InvalidEventDetailsException("The new event [" + eventName
                + "] has no description. Please describe what the event is about.");
    }

    public static InvalidEventDetailsException missingOptionName(String eventName) {
        return new InvalidEventDetailsException("The new event [" + eventName
                + "] is missing one of its options. Please name both possible outcomes.");
    }

    public static InvalidEventDetailsException sameOptionNames(String eventName, String optionName) {
        return new InvalidEventDetailsException("Both options of the new event [" + eventName
                + "] are named [" + optionName + "]. The two possible outcomes must be different.");
    }
}
