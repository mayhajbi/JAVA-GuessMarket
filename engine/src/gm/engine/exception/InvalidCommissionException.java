package gm.engine.exception;

/**
 * The commission of an event is not a legal percentage (0 - 90), or its type is unknown.
 */
public class InvalidCommissionException extends GuessMarketException {

    private static final long serialVersionUID = 1L;

    public InvalidCommissionException(String title, String message) {
        super(title, message);
    }

    public static InvalidCommissionException unknownType(String eventName, String type) {
        return new InvalidCommissionException("Invalid commission type", "'" + type + "' is not a valid commission type in "
                + describeEvent(eventName) + ". In the attribute 'type' of the element 'commission', use "
                + "'on-purchase' or 'on-close'.");
    }
}
