package gm.engine.exception;

/**
 * Base class of every error the engine reports to its callers.
 * <p>
 * All the exceptions of the engine are unchecked, so that a caller (any user interface layer) is
 * free to catch them in one single place instead of declaring many catch blocks.
 */
public abstract class GuessMarketException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    protected GuessMarketException(String message) {
        super(message);
    }

    protected GuessMarketException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * How every message names an event: its name in brackets and then its id, for example
     * "[World Cup Winner] (id 2)".
     */
    public static String describeEvent(String eventName, int eventId) {
        return "[" + eventName + "] (id " + eventId + ")";
    }
}
