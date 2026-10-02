package gm.engine.exception;

/**
 * A user tried to sell more shares than the user holds (and has not already offered in other orders).
 */
public class InsufficientSharesException extends GuessMarketException {

    private static final long serialVersionUID = 1L;

    public InsufficientSharesException(String optionName, String eventName,
                                       long requested, long available) {
        super("You cannot sell " + requested + " shares of '" + optionName + "' in " + describeEvent(eventName)
                + ". You have only " + available + " shares that are not already offered in other orders.");
    }
}
