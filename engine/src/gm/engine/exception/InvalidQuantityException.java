package gm.engine.exception;

/**
 * The requested amount of shares is not a positive number.
 */
public class InvalidQuantityException extends GuessMarketException {

    private static final long serialVersionUID = 1L;

    public InvalidQuantityException(long quantity) {
        super("Invalid quantity", "Enter a whole number of shares greater than zero. " + quantity + " is not valid.");
    }
}
