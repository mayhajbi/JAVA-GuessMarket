package gm.engine.exception;

/**
 * The commission of an event is not a legal percentage (0 - 90), or its type is unknown.
 */
public class InvalidCommissionException extends GuessMarketException {

    private static final long serialVersionUID = 1L;

    public InvalidCommissionException(String message) {
        super(message);
    }

    public static InvalidCommissionException outOfRange(String eventName, int value) {
        return new InvalidCommissionException("The commission of the event " + describeEvent(eventName)
                + " is " + value + ", which is not a legal percentage. "
                + "The commission must be a whole number from 0 to 90.");
    }

    public static InvalidCommissionException unknownType(String eventName, String type) {
        return new InvalidCommissionException("The attribute 'type' of the element 'commission' in the event "
                + describeEvent(eventName) + " is '" + type + "', which is not supported. "
                + "Please use 'on-purchase' or 'on-close'.");
    }
}
