package gm.engine.exception;

/**
 * The file is a well formed XML document, but it has a part that the system does not accept.
 */
public class UnsupportedFileFormatException extends GuessMarketException {

    private static final long serialVersionUID = 1L;

    public UnsupportedFileFormatException(String reason) {
        super("The format of the file is not supported: " + reason);
    }
}
