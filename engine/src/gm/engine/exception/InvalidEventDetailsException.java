package gm.engine.exception;

/**
 * The details of an event are not enough to create it. Mostly the details a user filled in for a new
 * event (bonus) - a data file reports a missing value with a message that points at the element that
 * is missing. Two options with the same name are reported this way for both.
 */
public class InvalidEventDetailsException extends GuessMarketException {

    private static final long serialVersionUID = 1L;

    private InvalidEventDetailsException(String title, String message) {
        super(title, message);
    }

    public static InvalidEventDetailsException notEnglish() {
        return new InvalidEventDetailsException("English only", "The name, description and options of an event must use "
                + "English letters, digits and common punctuation only.");
    }

    public static InvalidEventDetailsException missingName() {
        return new InvalidEventDetailsException("Name missing", "Enter a name for the event.");
    }

    public static InvalidEventDetailsException missingDescription(String eventName) {
        return new InvalidEventDetailsException("Description missing", "Enter a description for " + describeEvent(eventName) + ".");
    }

    public static InvalidEventDetailsException missingOptionName(String eventName) {
        return new InvalidEventDetailsException("Option missing", "Enter a name for both options of " + describeEvent(eventName) + ".");
    }

    public static InvalidEventDetailsException sameOptionNames(String eventName, String optionName) {
        return new InvalidEventDetailsException("Duplicate options", "Both options of " + describeEvent(eventName) + " are named '"
                + optionName + "'. Give the options different names. Names are not case-sensitive.");
    }
}
