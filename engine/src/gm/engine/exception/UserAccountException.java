package gm.engine.exception;

import java.util.Locale;

/**
 * An action cannot be done in the name of a user: the account is blocked, or it
 * does not hold the money or the shares the action needs.
 */
public class UserAccountException extends GuessMarketException {

    private static final long serialVersionUID = 1L;

    private UserAccountException(String title, String message) {
        super(title, message);
    }

    public static UserAccountException blocked(String action) {
        return new UserAccountException("Account blocked", "You cannot " + action + ", because your balance is below zero and you are blocked. Load funds "
                + "until your balance is zero or more.");
    }

    /**
     * @param costName what the market maker pays for when opening the event, for example "initial subsidy"
     */
    public static UserAccountException insufficientFunds(String eventName, String costName, double required,
                                                         double available) {
        return new UserAccountException("Insufficient funds", String.format(Locale.ROOT, "You cannot open %s. Opening it costs %.2f for the %s, but your "
                + "balance is %.2f. Load funds and try again.", describeEvent(eventName), required, costName,
                available));
    }

    public static UserAccountException insufficientShares(String optionName, String eventName, long requested,
                                                          long available) {
        return new UserAccountException("Insufficient shares", "You cannot sell " + requested + " shares of '" + optionName + "' in " + describeEvent(eventName)
                + ". You have only " + available + " shares that are not already offered in other orders.");
    }
}
