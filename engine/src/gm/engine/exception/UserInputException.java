package gm.engine.exception;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * Something a user typed or chose is not accepted: a text that is empty or not in English, a name
 * that is already taken, or an amount, quantity or price that is not valid.
 */
public class UserInputException extends GuessMarketException {

    private static final long serialVersionUID = 1L;

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

    public static UserInputException emptyField(String fieldName) {
        return new UserInputException("Empty field", "The " + fieldName + " cannot be empty. Fill it in and try again.");
    }

    public static UserInputException notPositive(String fieldName) {
        return new UserInputException("Invalid number", "The " + fieldName + " must be greater than 0.");
    }

    public static UserInputException outOfRange(String fieldName, int min, int max) {
        return outOfRange(fieldName, String.valueOf(min), String.valueOf(max));
    }

    public static UserInputException outOfRange(String fieldName, double min, double max) {
        return outOfRange(fieldName, String.format(Locale.ROOT, "%.2f", min), String.format(Locale.ROOT, "%.2f", max));
    }

    public static UserInputException priceNotWholeCents(double price) {
        // The price is shown as it was given: rounded to two digits it would hide what is wrong with it.
        return new UserInputException("Invalid price", "The price " + new BigDecimal(String.valueOf(price)).toPlainString()
                + " cannot be accepted. A price must be a whole number of cents, and can have at most two decimal "
                + "places, for example 0.45.");
    }

    private static UserInputException outOfRange(String fieldName, String min, String max) {
        return new UserInputException("Number out of range", "The number entered in " + fieldName
                + " is out of range. Choose a number between " + min + " and " + max + ".");
    }
}
