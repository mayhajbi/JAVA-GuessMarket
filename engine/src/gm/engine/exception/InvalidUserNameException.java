package gm.engine.exception;

/**
 * A user tried to register without giving a name.
 */
public class InvalidUserNameException extends GuessMarketException {

    private static final long serialVersionUID = 1L;

    public InvalidUserNameException() {
        super("No user name was given. Please enter a name to log in.");
    }
}
