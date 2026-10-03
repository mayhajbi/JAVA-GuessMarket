package gm.engine.exception;

/**
 * A file of events cannot be loaded: it is not an XML file, it cannot be read, it has something the
 * system does not accept, or it is missing a value it needs.
 */
public class FileLoadException extends GuessMarketException {

    private static final long serialVersionUID = 1L;

    private FileLoadException(String title, String message) {
        super(title, message);
    }

    private FileLoadException(String title, String message, Throwable cause) {
        super(title, message, cause);
    }

    public static FileLoadException invalidPath(String message) {
        return new FileLoadException("Invalid file", message);
    }

    /**
     * @param reason what the file has, to follow "because", for example "the file has the element 'GM-users'"
     * @param rule   the rule the file breaks, as a full sentence
     */
    public static FileLoadException unsupportedFormat(String reason, String rule) {
        return new FileLoadException("Unsupported file", "The file cannot be used, because " + reason + ". " + rule
                + " Remove it and upload the file again.");
    }

    public static FileLoadException unreadable(String filePath, String reason, Throwable cause) {
        return new FileLoadException("File not readable", "The file '" + filePath
                + "' could not be read as a Guess Market XML file. Reason: " + reason, cause);
    }

    /**
     * @param location where the element is expected, for example "the event 'World Cup Winner'"
     */
    public static FileLoadException missingElement(String name, String location) {
        return missing("element '" + name + "'", location);
    }

    /**
     * For a place where one of two elements is expected.
     */
    public static FileLoadException missingEitherElement(String first, String second, String location) {
        return missing("element '" + first + "' or '" + second + "'", location);
    }

    /**
     * @param element the element the attribute belongs to
     */
    public static FileLoadException missingAttribute(String name, String element, String location) {
        return missing("attribute '" + name + "' of the element '" + element + "'", location);
    }

    public static FileLoadException wrongOptionCount() {
        return new FileLoadException("Invalid options", "An event must have exactly 2 options, no more and no fewer.");
    }

    public static FileLoadException unknownCommissionType(String eventName, String type) {
        return new FileLoadException("Invalid commission type", "'" + type + "' is not a valid commission type in "
                + describeEvent(eventName) + ". In the attribute 'type' of the element 'commission', use "
                + "'on-purchase' or 'on-close'.");
    }

    public static FileLoadException allowMintNotBoolean(String eventName, String value) {
        return new FileLoadException("Invalid minting setting", "'" + value + "' is not valid for the attribute 'allow-mint' of "
                + describeEvent(eventName) + ". Use 'true' or 'false'.");
    }

    private static FileLoadException missing(String missing, String location) {
        return new FileLoadException("Missing data in file", "The " + missing + " is missing or empty in " + location
                + ". Add it and upload the file again.");
    }
}
