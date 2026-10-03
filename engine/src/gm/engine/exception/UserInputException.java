package gm.engine.exception;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * Something a user typed or chose is not accepted: a text that is empty or not in English, a name
 * that is already taken, or an amount, quantity or price that is not valid.
 */
public class UserInputException extends GuessMarketException {

    private static final long serialVersionUID = 1L;

    private static final double MIN_PRICE = 0.01;

    /** What went wrong, for a caller that reacts differently to a name that is taken. */
    public enum Kind {
        UNAVAILABLE_NAME,
        OTHER
    }

    private final Kind kind;

    private UserInputException(Kind kind, String title, String message) {
        super(title, message);
        this.kind = kind;
    }

    private UserInputException(String title, String message) {
        this(Kind.OTHER, title, message);
    }

    public Kind getKind() {
        return kind;
    }

    public static UserInputException notEnglish() {
        return new UserInputException("English only", "Use English letters, digits and common punctuation only.");
    }

    public static UserInputException unavailableName(String name) {
        return new UserInputException(Kind.UNAVAILABLE_NAME, "Name not available",
                "'" + name + "' is already taken. Please choose another.");
    }

    public static UserInputException missingUserName() {
        return new UserInputException("User name missing", "Enter a user name to log in.");
    }

    public static UserInputException emptyChatLine() {
        return new UserInputException("Empty message", "Write a message before sending.");
    }

    public static UserInputException invalidDeposit(double amount) {
        return new UserInputException("Invalid amount",
                String.format(Locale.ROOT, "Enter an amount greater than zero. %.2f cannot be loaded.", amount));
    }

    public static UserInputException invalidQuantity(long quantity) {
        return new UserInputException("Invalid quantity",
                "Enter a whole number of shares greater than zero. " + quantity + " is not valid.");
    }

    public static UserInputException incompleteOrder() {
        return new UserInputException("Incomplete order", "The order is incomplete. It needs an event, a side, an option, a "
                + "quantity and a price.");
    }

    public static UserInputException missingSide(String eventName) {
        return new UserInputException("Incomplete order", "Choose Buy or Sell for the order in " + describeEvent(eventName) + ".");
    }

    public static UserInputException priceOutOfRange(String eventName, double price, int baseValue) {
        String problem = Double.isNaN(price) ? "not valid" : price < MIN_PRICE ? "too low" : "too high";
        return new UserInputException("Invalid price", String.format(Locale.ROOT, "A price of %.2f is %s for %s. A winning "
                + "share pays %.2f, so the price must be between %.2f and %.2f.", price, problem,
                describeEvent(eventName), (double) baseValue, MIN_PRICE, baseValue - MIN_PRICE));
    }

    public static UserInputException priceNotWholeCents(double price) {
        // The price is shown as it was given: rounded to two digits it would hide what is wrong with it.
        return new UserInputException("Invalid price", "The price " + new BigDecimal(String.valueOf(price)).toPlainString()
                + " has more than two decimal places. Enter it in whole cents, for example 0.45.");
    }
}
