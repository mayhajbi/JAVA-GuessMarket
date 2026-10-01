package gm.engine.exception;

import java.util.Locale;

/**
 * A user does not have enough money for an action that must be fully covered in advance (a market
 * maker opening an event).
 */
public class InsufficientFundsException extends GuessMarketException {

    private static final long serialVersionUID = 1L;

    /**
     * @param costName what the market maker pays for when opening the event, for example "initial subsidy"
     */
    public InsufficientFundsException(String userName, String eventName, String costName, double required,
                                      double available) {
        super(String.format(Locale.ROOT, "The user '%s' cannot open the event %s. Opening it costs the %s of "
                + "%.2f, but the balance of the user is only %.2f. Please load funds and try again.", userName,
                describeEvent(eventName), costName, required, available));
    }
}
