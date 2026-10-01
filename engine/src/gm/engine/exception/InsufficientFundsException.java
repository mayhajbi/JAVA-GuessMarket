package gm.engine.exception;

import java.util.Locale;

/**
 * A user does not have enough money for an action that must be fully covered in advance (a market
 * maker opening an event).
 */
public class InsufficientFundsException extends GuessMarketException {

    private static final long serialVersionUID = 1L;

    public InsufficientFundsException(String userName, String action, double required,
                                      double available) {
        super(String.format(Locale.ROOT, "The user '%s' cannot %s: %.2f is required, but the balance is "
                + "only %.2f.", userName, action, required, available));
    }
}
