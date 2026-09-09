package gm.engine.exception;

/**
 * The data file defines two users with the same name (compared without case).
 */
public class DuplicateUserNameException extends GuessMarketException {

    private static final long serialVersionUID = 1L;

    public DuplicateUserNameException(String name) {
        super("A user named '" + name + "' is already defined. User names must be unique.");
    }
}
