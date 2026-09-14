package gm.engine.exception;

/**
 * The commission of an event is not a legal percentage (0 - 90), or its type is unknown.
 */
public class InvalidCommissionException extends GuessMarketException {

    private static final long serialVersionUID = 1L;

    public InvalidCommissionException(String message) {
        super(message);
    }

    public static InvalidCommissionException outOfRange(int id, String eventName, int value) {
        return new InvalidCommissionException("The commission of the event " + describeEvent(eventName, id)
                + " is " + value + ", which is not a legal percentage. "
                + "The commission must be an integer between 0 and 90 (inclusive).");
    }

    public static InvalidCommissionException unknownType(int id, String eventName, String type) {
        return new InvalidCommissionException("The commission type of the event "
                + describeEvent(eventName, id) + " is [" + type + "], which is not supported. "
                + "The supported types are [on-purchase] and [on-close].");
    }
}
