package gm.engine.exception;

/**
 * A parameter of a {@code <GM-order-book>} method is not legal: the base value (d) is not positive,
 * the initial investment is negative, or allow-mint is not a boolean.
 */
public class InvalidOrderBookException extends GuessMarketException {

    private static final long serialVersionUID = 1L;

    private InvalidOrderBookException(String message) {
        super(message);
    }

    public static InvalidOrderBookException baseValueNotPositive(int id, String eventName, int d) {
        return new InvalidOrderBookException("The base value (d) of the order book event ["
                + eventName + "] (id " + id + ") is " + d
                + ". It must be a positive integer (greater than 0).");
    }

    public static InvalidOrderBookException initialInvestmentNegative(int id, String eventName,
                                                                     int initial) {
        return new InvalidOrderBookException("The initial investment of the order book event ["
                + eventName + "] (id " + id + ") is " + initial + ". It cannot be negative.");
    }

    public static InvalidOrderBookException allowMintNotBoolean(int id, String eventName,
                                                               String value) {
        return new InvalidOrderBookException("The allow-mint value of the order book event ["
                + eventName + "] (id " + id + ") is [" + value
                + "]. It must be either [true] or [false].");
    }
}
