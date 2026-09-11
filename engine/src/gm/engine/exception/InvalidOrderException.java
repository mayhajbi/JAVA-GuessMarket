package gm.engine.exception;

import java.util.Locale;

/**
 * An order cannot be placed as requested: its price is outside the legal range or not in whole cents,
 * or a detail of it is missing.
 */
public class InvalidOrderException extends GuessMarketException {

    private static final long serialVersionUID = 1L;

    private InvalidOrderException(String message) {
        super(message);
    }

    public static InvalidOrderException missingRequest() {
        return new InvalidOrderException("No order details were given. An order needs an event, a user, "
                + "a side (buy or sell), an option, a quantity and a price.");
    }

    public static InvalidOrderException missingSide(int eventId, String eventName) {
        return new InvalidOrderException("The order for the event [" + eventName + "] (id " + eventId
                + ") does not say whether to buy or to sell. Please choose Buy or Sell.");
    }

    public static InvalidOrderException priceOutOfRange(int eventId, String eventName, double price,
                                                        int baseValue) {
        return new InvalidOrderException(String.format(Locale.ROOT, "The price %s is not legal for the "
                + "event [%s] (id %d). The price per share must be between 0.01 and %.2f (the base "
                + "value d of the event is %d).", price, eventName, eventId, baseValue - 0.01, baseValue));
    }

    public static InvalidOrderException priceNotWholeCents(int eventId, String eventName, double price) {
        return new InvalidOrderException(String.format(Locale.ROOT, "The price %s for the event [%s] (id "
                + "%d) has more than two digits after the decimal point. Prices are set in whole cents, "
                + "for example 0.45.", price, eventName, eventId));
    }
}
