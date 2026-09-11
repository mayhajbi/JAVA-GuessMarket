package gm.engine.exception;

/**
 * A blocked user (whose balance dropped below zero at some point) tried to start a new action.
 */
public class UserBlockedException extends GuessMarketException {

    private static final long serialVersionUID = 1L;

    public UserBlockedException(String userName, String action) {
        super("The user [" + userName + "] cannot " + action + ": the balance of the user dropped "
                + "below zero, so the user is blocked from any further action.");
    }
}
