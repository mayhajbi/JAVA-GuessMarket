package gm.engine.exception;

/**
 * A user tried to register without giving a name.
 */
public class InvalidUserNameException extends GuessMarketException {

    private static final long serialVersionUID = 1L;

    public InvalidUserNameException() {
        super("Enter a user name to log in.");
    }

    private InvalidUserNameException(String message) {
        super(message);
    }

    public static InvalidUserNameException notEnglish() {
        return new InvalidUserNameException("The user name must use English letters, digits and common "
                + "punctuation only.");
    }
}
