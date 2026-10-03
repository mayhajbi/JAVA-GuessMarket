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

    public static UserAccountException blocked() {
        return new UserAccountException("Account blocked", "Your account is blocked. While your account balance is "
                + "below 0, you cannot perform actions that require a payment.");
    }

    public static UserAccountException insufficientFunds(double required, double available) {
        return new UserAccountException("Insufficient funds", String.format(Locale.ROOT, "The action cannot be "
                + "completed because your account balance is too low. You are short by %.2f. Try again when you have "
                + "enough funds.", required - available));
    }
}
