package gm.engine.exception;

/**
 * No user with the requested name exists in the system.
 */
public class UserNotFoundException extends GuessMarketException {

    private static final long serialVersionUID = 1L;

    public UserNotFoundException(String name) {
        super("There is no user named '" + name + "' in the system.");
    }
}
