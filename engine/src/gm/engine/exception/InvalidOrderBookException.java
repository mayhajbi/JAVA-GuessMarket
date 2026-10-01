package gm.engine.exception;

/**
 * A parameter of a {@code <GM-order-book>} method is not legal: the base value (d) is not positive,
 * the initial investment is negative or not a multiple of d, or allow-mint is not a boolean.
 */
public class InvalidOrderBookException extends GuessMarketException {

    private static final long serialVersionUID = 1L;

    private InvalidOrderBookException(String message) {
        super(message);
    }

    public static InvalidOrderBookException baseValueNotPositive(String eventName, int d) {
        return new InvalidOrderBookException("The base value of the order book event "
                + describeEvent(eventName) + " is " + d
                + ". It must be a positive whole number. In a file it is the attribute 'd'.");
    }

    public static InvalidOrderBookException initialInvestmentNegative(String eventName,
                                                                     int initial) {
        return new InvalidOrderBookException("The initial investment of the order book event "
                + describeEvent(eventName) + " is " + initial + ". It cannot be negative. In a file it "
                + "is the attribute 'initial'.");
    }

    public static InvalidOrderBookException initialNotDivisible(String eventName, int initial,
                                                               int d) {
        return new InvalidOrderBookException("The initial investment of the order book event "
                + describeEvent(eventName) + " is " + initial + ", which is not a multiple of its base "
                + "value " + d + ". The market maker receives one pair of shares for every " + d
                + ", so the initial investment must divide by " + d + " without a remainder. In a file these "
                + "are the attributes 'initial' and 'd'.");
    }

    public static InvalidOrderBookException allowMintNotBoolean(String eventName,
                                                               String value) {
        return new InvalidOrderBookException("The attribute 'allow-mint' of the order book event "
                + describeEvent(eventName) + " is '" + value + "'. It must be either 'true' or 'false'.");
    }
}
