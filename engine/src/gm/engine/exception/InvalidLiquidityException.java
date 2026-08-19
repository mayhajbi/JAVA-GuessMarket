package gm.engine.exception;

/**
 * The liquidity parameter (b) of an LMSR event is not a positive number.
 */
public class InvalidLiquidityException extends GuessMarketException {

    private static final long serialVersionUID = 1L;

    public InvalidLiquidityException(int id, String eventName, int b) {
        super("The liquidity value (b) of the LMSR event [" + eventName + "] (id " + id + ") is "
                + b + ". The liquidity value must be a positive integer (greater than 0).");
    }
}
