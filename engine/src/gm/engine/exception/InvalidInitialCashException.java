package gm.engine.exception;

/**
 * The initial cash of a user in the data file is not a positive amount.
 */
public class InvalidInitialCashException extends GuessMarketException {

    private static final long serialVersionUID = 1L;

    public InvalidInitialCashException(String userName, int value) {
        super("The initial cash of the user [" + userName + "] is " + value
                + ". Every user must start with a positive amount of cash (greater than 0).");
    }
}
