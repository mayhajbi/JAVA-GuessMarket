package gm.engine.exception;

/**
 * The options of an event in the data file are not legal for this system.
 */
public class InvalidOptionsException extends GuessMarketException {

    private static final long serialVersionUID = 1L;

    public InvalidOptionsException(int id, String eventName, int optionsFound) {
        super("The event [" + eventName + "] (id " + id + ") has " + optionsFound
                + " option(s). Every event in the system must have exactly 2 possible options.");
    }
}
