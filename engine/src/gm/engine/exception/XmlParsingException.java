package gm.engine.exception;

/**
 * The file could not be read as an XML document that describes a Guess Market system.
 */
public class XmlParsingException extends GuessMarketException {

    private static final long serialVersionUID = 1L;

    public XmlParsingException(String filePath, String reason, Throwable cause) {
        super("The file [" + filePath + "] could not be read as a Guess Market XML file. Reason: "
                + reason, cause);
    }
}
