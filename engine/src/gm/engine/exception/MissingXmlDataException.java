package gm.engine.exception;

/**
 * A mandatory element or attribute is missing (or empty) in the data file.
 */
public class MissingXmlDataException extends GuessMarketException {

    private static final long serialVersionUID = 1L;

    private MissingXmlDataException(String missing, String location) {
        super("Missing data in file", "The " + missing + " is missing or empty in " + location + ". Add it and upload the file again.");
    }

    /**
     * @param location where the element is expected, for example "the event 'World Cup Winner'"
     */
    public static MissingXmlDataException element(String name, String location) {
        return new MissingXmlDataException("element '" + name + "'", location);
    }

    /**
     * For a place where one of two elements is expected.
     */
    public static MissingXmlDataException eitherElement(String first, String second, String location) {
        return new MissingXmlDataException("element '" + first + "' or '" + second + "'", location);
    }

    /**
     * @param element the element the attribute belongs to
     */
    public static MissingXmlDataException attribute(String name, String element, String location) {
        return new MissingXmlDataException("attribute '" + name + "' of the element '" + element + "'", location);
    }
}
