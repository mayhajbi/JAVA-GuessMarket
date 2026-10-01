package gm.engine.exception;

/**
 * The file is a well formed XML document, but it has a part that the system does not accept.
 */
public class UnsupportedFileFormatException extends GuessMarketException {

    private static final long serialVersionUID = 1L;

    /**
     * @param reason what the file has, to follow "because", for example "the file has the element 'GM-users'"
     * @param rule   the rule the file breaks, as a full sentence
     */
    public UnsupportedFileFormatException(String reason, String rule) {
        super("The format of the file is not supported, because " + reason + ". " + rule
                + " Please remove the element and upload the file again.");
    }
}
