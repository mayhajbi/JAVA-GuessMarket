package gm.engine.exception;

/**
 * A user tried to sell more shares than the user holds (and has not already offered in other orders).
 */
public class InsufficientSharesException extends GuessMarketException {

    private static final long serialVersionUID = 1L;

    public InsufficientSharesException(String userName, String optionName, String eventName,
                                       long requested, long available) {
        super("The user [" + userName + "] cannot sell " + requested + " shares of [" + optionName
                + "] in the event [" + eventName + "]: the user has only " + available + " shares of "
                + "it that are not already offered for sale in other orders.");
    }
}
