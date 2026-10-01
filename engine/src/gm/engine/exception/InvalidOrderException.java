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

    private InvalidOrderException(String message) {
        super(message);
    }

    public static InvalidOrderException missingRequest() {
        return new InvalidOrderException("No order details were given. An order needs an event, a user, "
                + "a side, an option, a quantity and a price.");
    }

    public static InvalidOrderException missingSide(String eventName) {
        return new InvalidOrderException("The order for the event " + describeEvent(eventName)
                + " does not say whether to buy or to sell. Please choose Buy or Sell.");
    }

    public static InvalidOrderException priceOutOfRange(String eventName, double price,
                                                        int baseValue) {
        String problem = Double.isNaN(price) ? "not legal" : price < MIN_PRICE ? "too low" : "too high";
        return new InvalidOrderException(String.format(Locale.ROOT, "The price %.2f is %s for the event %s. "
                + "A winning share in this event pays %.2f, so the price per share must be between %.2f and "
                + "%.2f.", price, problem, describeEvent(eventName), (double) baseValue, MIN_PRICE,
                baseValue - MIN_PRICE));
    }

    public static InvalidOrderException priceNotWholeCents(String eventName, double price) {
        // The price is shown as it was given: rounded to two digits it would hide what is wrong with it.
        return new InvalidOrderException("The price " + new BigDecimal(String.valueOf(price)).toPlainString()
                + " for the event " + describeEvent(eventName) + " has more than two digits after the decimal "
                + "point. Please enter a price in whole cents, for example 0.45.");
    }
}
