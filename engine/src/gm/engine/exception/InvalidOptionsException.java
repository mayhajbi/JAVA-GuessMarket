package gm.engine.exception;

/**
 * The options of an event in the data file are not legal for this system.
 */
public class InvalidOptionsException extends GuessMarketException {

    private static final long serialVersionUID = 1L;

    public InvalidOptionsException() {
        super("Invalid options", "An event must have exactly 2 options, no more and no fewer.");
    }
}
