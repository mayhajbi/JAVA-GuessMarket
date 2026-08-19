package gm.engine.exception;

/**
 * The given path does not point to a readable XML file.
 */
public class InvalidFilePathException extends GuessMarketException {

    private static final long serialVersionUID = 1L;

    public InvalidFilePathException(String message) {
        super(message);
    }
}
