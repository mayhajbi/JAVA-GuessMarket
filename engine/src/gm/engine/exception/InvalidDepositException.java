package gm.engine.exception;

/**
 * The amount a user asked to deposit into the account is not a positive number.
 */
public class InvalidDepositException extends GuessMarketException {

    private static final long serialVersionUID = 1L;

    public InvalidDepositException(double amount) {
        super("The amount to deposit is " + amount + ". A deposit must be a positive number.");
    }
}
