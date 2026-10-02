package gm.engine.exception;

import java.util.Locale;

/**
 * The amount a user asked to deposit into the account is not a positive number.
 */
public class InvalidDepositException extends GuessMarketException {

    private static final long serialVersionUID = 1L;

    public InvalidDepositException(double amount) {
        super(String.format(Locale.ROOT, "Enter an amount greater than zero. %.2f cannot be loaded.", amount));
    }
}
