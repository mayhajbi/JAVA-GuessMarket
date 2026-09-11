package gm.ui.fx.common;

import gm.dto.CommissionType;

import java.util.Locale;

/**
 * The text formats shared by all the screens.
 */
public final class Formats {

    private Formats() {
    }

    /**
     * Money, prices and option values are always shown with exactly two digits after the point.
     */
    public static String decimal(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    /**
     * Like {@link #decimal(double)}, for a value that may not exist yet (shown as "-").
     */
    public static String optionalDecimal(Double value) {
        return value == null ? "-" : decimal(value);
    }

    public static String commission(int percent, CommissionType type) {
        return percent + "% " + type.getDisplayName();
    }
}
