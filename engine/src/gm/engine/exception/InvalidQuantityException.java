package gm.engine.exception;

/**
 * The requested amount of shares is not a positive number.
 */
public class InvalidQuantityException extends GuessMarketException {

    private static final long serialVersionUID = 1L;

    public InvalidQuantityException(long quantity) {
        super("The requested amount of shares is " + quantity
                + ". The amount of shares to buy or sell must be a positive number (1 or more).");
    }
}
