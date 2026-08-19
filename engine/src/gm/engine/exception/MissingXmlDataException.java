package gm.engine.exception;

/**
 * A mandatory element or attribute is missing (or empty) in the data file.
 */
public class MissingXmlDataException extends GuessMarketException {

    private static final long serialVersionUID = 1L;

    public MissingXmlDataException(String elementName, String location) {
        super("The mandatory element [" + elementName + "] is missing or empty in " + location
                + ". Please add it to the file and load it again.");
    }
}
