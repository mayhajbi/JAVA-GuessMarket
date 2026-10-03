package gm.engine.exception;

/**
 * A username that is already taken (compared without case), for example at login.
 */
public class DuplicateUserNameException extends GuessMarketException {

    private static final long serialVersionUID = 1L;

    public DuplicateUserNameException(String name) {
        super("Username is not available",
                "'" + name + "' is already taken. Please choose another.");
    }
}
