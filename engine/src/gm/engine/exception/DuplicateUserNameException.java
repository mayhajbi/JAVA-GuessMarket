package gm.engine.exception;

/**
 * A user name that is already taken (compared without case), for example at login.
 */
public class DuplicateUserNameException extends GuessMarketException {

    private static final long serialVersionUID = 1L;

    public DuplicateUserNameException(String name) {
        super("A user named '" + name + "' is already defined. Please choose another user name.");
    }
}
