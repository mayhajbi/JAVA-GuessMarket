package gm.engine.exception;

/**
 * A blocked user, whose balance is below zero, tried to start a new action.
 */
public class UserBlockedException extends GuessMarketException {

    private static final long serialVersionUID = 1L;

    public UserBlockedException(String action) {
        super("Account blocked", "You cannot " + action + ", because your balance is below zero and you are blocked. Load funds "
                + "until your balance is zero or more.");
    }
}
