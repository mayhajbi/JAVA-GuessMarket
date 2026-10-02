package gm.engine.exception;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * An order cannot be placed as requested: its price is outside the legal range or not in whole cents,
 * or a detail of it is missing.
 */
public class InvalidOrderException extends GuessMarketException {

    private static final long serialVersionUID = 1L;

    private static final double MIN_PRICE = 0.01;

    private InvalidOrderException(String title, String message) {
        super(title, message);
    }

    public static InvalidOrderException missingRequest() {
        return new InvalidOrderException("Incomplete order", "The order is incomplete. It needs an event, a side, an option, a "
                + "quantity and a price.");
    }

    public static InvalidOrderException missingSide(String eventName) {
        return new InvalidOrderException("Incomplete order", "Choose Buy or Sell for the order in " + describeEvent(eventName) + ".");
    }

    public static InvalidOrderException priceOutOfRange(String eventName, double price,
                                                        int baseValue) {
        String problem = Double.isNaN(price) ? "not valid" : price < MIN_PRICE ? "too low" : "too high";
        return new InvalidOrderException("Invalid price", String.format(Locale.ROOT, "A price of %.2f is %s for %s. A winning "
                + "share pays %.2f, so the price must be between %.2f and %.2f.", price, problem,
                describeEvent(eventName), (double) baseValue, MIN_PRICE, baseValue - MIN_PRICE));
    }

    public static InvalidOrderException priceNotWholeCents(String eventName, double price) {
        // The price is shown as it was given: rounded to two digits it would hide what is wrong with it.
        return new InvalidOrderException("Invalid price", "The price " + new BigDecimal(String.valueOf(price)).toPlainString()
                + " has more than two decimal places. Enter it in whole cents, for example 0.45.");
    }
}
