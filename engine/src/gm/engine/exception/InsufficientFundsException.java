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
    public InsufficientFundsException(String eventName, String costName, double required,
                                      double available) {
        super("Insufficient funds", String.format(Locale.ROOT, "You cannot open %s. Opening it costs %.2f for the %s, but your "
                + "balance is %.2f. Load funds and try again.", describeEvent(eventName), required, costName,
                available));
    }
}
