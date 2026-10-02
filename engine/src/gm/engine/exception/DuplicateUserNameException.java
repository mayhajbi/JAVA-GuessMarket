package gm.engine.exception;

/**
 * A user name that is already taken (compared without case), for example at login.
 */
public class DuplicateUserNameException extends GuessMarketException {

    private static final long serialVersionUID = 1L;

    public DuplicateUserNameException(String name) {
        super("User name taken", "The user name '" + name + "' is already taken. Choose a different name.");
    }
}
