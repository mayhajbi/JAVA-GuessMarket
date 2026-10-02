package gm.engine.exception;

/**
 * The options of an event in the data file are not legal for this system.
 */
public class InvalidOptionsException extends GuessMarketException {

    private static final long serialVersionUID = 1L;

    public InvalidOptionsException(String eventName, int optionsFound) {
        super(describeEvent(eventName) + " has " + optionsFound
                + " options. An event must have exactly 2, each in a 'GM-option' element.");
    }
}
