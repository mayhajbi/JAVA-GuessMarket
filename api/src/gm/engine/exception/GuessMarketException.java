package gm.engine.exception;

/**
 * Base class of every error the engine reports to its callers.
 * <p>
 * All the exceptions of the engine are unchecked, so that a caller (any user interface layer) is
 * free to catch them in one single place instead of declaring many catch blocks.
 */
public abstract class GuessMarketException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String title;

    /**
     * @param title   what went wrong, in a few words: the title of the dialog that shows the error
     * @param message what the user can do about it
     */
    protected GuessMarketException(String title, String message) {
        super(message);
        this.title = title;
    }

    protected GuessMarketException(String title, String message, Throwable cause) {
        super(message, cause);
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    /**
     * How every message names an event: its name between single quotes, for example 'World Cup Winner'.
     */
    public static String describeEvent(String eventName) {
        return "'" + eventName + "'";
    }
}
