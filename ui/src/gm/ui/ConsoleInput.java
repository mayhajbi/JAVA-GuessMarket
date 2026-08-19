package gm.ui;

import java.util.Scanner;
import java.util.function.LongFunction;
import java.util.function.LongPredicate;

/**
 * Collects input from the user and makes sure it is valid before it is passed on to the engine.
 * <p>
 * Every method keeps asking the user until a legal value is entered, so an illegal input never stops
 * the system.
 */
class ConsoleInput {

    private final Scanner scanner = new Scanner(System.in);

    /**
     * Reads a non-empty line of text and passes it on as is; any further interpretation of it (such
     * as a file path) is the engine's responsibility, not the UI's.
     */
    String readText(String prompt) {
        while (true) {
            System.out.println(prompt);
            String text = scanner.nextLine().trim();
            if (!text.isEmpty()) {
                return text;
            }
            System.out.println("An empty value was entered. Please enter a value.");
        }
    }

    /**
     * Reads a whole number between the given boundaries (inclusive).
     */
    int readNumberInRange(String prompt, int minValue, int maxValue) {
        long value = readWholeNumber(prompt,
                candidate -> candidate >= minValue && candidate <= maxValue,
                candidate -> "The number " + candidate + " is not one of the choices. Please enter "
                        + "a number between " + minValue + " and " + maxValue + ".",
                "a number between " + minValue + " and " + maxValue);
        return (int) value;
    }

    /**
     * Reads a positive whole amount (1 or more).
     */
    long readPositiveAmount(String prompt) {
        return readWholeNumber(prompt,
                candidate -> candidate > 0,
                candidate -> "The amount must be a positive number. Please enter 1 or more.",
                "a positive whole number");
    }

    /**
     * Shared retry loop for {@link #readNumberInRange} and {@link #readPositiveAmount}: keeps asking
     * until a value that parses as a whole number and satisfies {@code isValid} is entered.
     */
    private long readWholeNumber(String prompt, LongPredicate isValid,
            LongFunction<String> invalidValueMessage, String expectedFormat) {
        while (true) {
            System.out.println(prompt);
            String text = scanner.nextLine().trim();
            try {
                long value = Long.parseLong(text);
                if (isValid.test(value)) {
                    return value;
                }
                System.out.println(invalidValueMessage.apply(value));
            } catch (NumberFormatException exception) {
                System.out.println("[" + text + "] is not a whole number. Please enter " + expectedFormat
                        + ".");
            }
        }
    }
}
