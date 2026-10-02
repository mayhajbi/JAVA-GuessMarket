package gm.engine.exception;

/**
 * No file was given, or its name does not end with the XML extension.
 */
public class InvalidFilePathException extends GuessMarketException {

    private static final long serialVersionUID = 1L;

    public InvalidFilePathException(String message) {
        super("Invalid file", message);
    }
}
