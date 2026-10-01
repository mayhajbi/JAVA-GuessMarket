package gm.engine.exception;

/**
 * The details of an event are not enough to create it. Mostly the details a user filled in for a new
 * event (bonus) - a data file reports a missing value with a message that points at the element that
 * is missing. Two options with the same name are reported this way for both.
 */
public class InvalidEventDetailsException extends GuessMarketException {

    private static final long serialVersionUID = 1L;

    private InvalidEventDetailsException(String message) {
        super(message);
    }

    public static InvalidEventDetailsException notEnglish() {
        return new InvalidEventDetailsException("An event has to be written in English. Please write its name, "
                + "its description and its options using English letters, digits and common punctuation marks "
                + "only.");
    }

    public static InvalidEventDetailsException missingName() {
        return new InvalidEventDetailsException("The new event has no name. "
                + "Please give the event a name.");
    }

    public static InvalidEventDetailsException missingDescription(String eventName) {
        return new InvalidEventDetailsException("The new event '" + eventName + "' has no description. Please describe what the event is about.");
    }

    public static InvalidEventDetailsException missingOptionName(String eventName) {
        return new InvalidEventDetailsException("The new event '" + eventName + "' is missing one of its options. Please name both possible outcomes.");
    }

    public static InvalidEventDetailsException sameOptionNames(String eventName, String optionName) {
        return new InvalidEventDetailsException("Both options of the event " + describeEvent(eventName)
                + " are named '" + optionName + "'. The two options of an event must have different names, "
                + "and names that differ only in upper and lower case letters count as the same name.");
    }
}
