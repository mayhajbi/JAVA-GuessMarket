package gm.ui.fx.common;

import gm.dto.CommissionType;

import java.util.Locale;

/**
 * The text formats shared by all the screens.
 */
public final class Formats {

    /** What is shown for a value that does not exist yet. */
    public static final String NOT_AVAILABLE = "-";

    private Formats() {
    }

    /**
     * Money, prices and option values are always shown with exactly two digits after the point.
     */
    public static String decimal(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    /**
     * Like {@link #decimal(double)}, for a value that may not exist yet (shown as {@value #NOT_AVAILABLE}).
     */
    public static String optionalDecimal(Double value) {
        return value == null ? NOT_AVAILABLE : decimal(value);
    }

    public static String commission(int percent, CommissionType type) {
        return percent + "% " + type.getDisplayName();
    }

    /**
     * An option the way the user sees it, numbered from 1 - for example "1. Yes".
     */
    public static String numberedOption(int number, String optionName) {
        return number + ". " + optionName;
    }
}
