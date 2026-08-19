package gm.engine.exception;

/**
 * Saving or loading the state of the system to/from a file has failed.
 */
public class SystemStateFileException extends GuessMarketException {

    private static final long serialVersionUID = 1L;

    public SystemStateFileException(String message, Throwable cause) {
        super(message, cause);
    }

    public SystemStateFileException(String message) {
        super(message);
    }
}
