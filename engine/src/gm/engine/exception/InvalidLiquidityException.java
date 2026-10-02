package gm.engine.exception;

/**
 * The liquidity parameter (b) of an LMSR event is not a positive number.
 */
public class InvalidLiquidityException extends GuessMarketException {

    private static final long serialVersionUID = 1L;

    public InvalidLiquidityException(String eventName, int b) {
        super("Invalid liquidity", "The liquidity of " + describeEvent(eventName) + " is " + b
                + ". It must be a positive whole number. In a file, it is the element 'b'.");
    }
}
