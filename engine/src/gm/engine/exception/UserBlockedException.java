package gm.engine.exception;

/**
 * A blocked user, whose balance is below zero, tried to start a new action.
 */
public class UserBlockedException extends GuessMarketException {

    private static final long serialVersionUID = 1L;

    public UserBlockedException(String userName, String action) {
        super("The user '" + userName + "' cannot " + action + ", because the balance of the user is below "
                + "zero and the user is blocked. Please load funds until the balance is zero or more.");
    }
}
