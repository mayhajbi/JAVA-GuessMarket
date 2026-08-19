package gm.engine.exception;

/**
 * An action that requires loaded data was requested before any valid file was loaded.
 */
public class NoSystemLoadedException extends GuessMarketException {

    private static final long serialVersionUID = 1L;

    public NoSystemLoadedException() {
        super("No events are loaded in the system yet. Please load a valid XML events file first "
                + "and then try again.");
    }
}
