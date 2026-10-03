package gm.engine.core;

import gm.engine.exception.EventException;
import gm.engine.exception.FileLoadException;
import gm.engine.exception.UserInputException;
import gm.engine.util.InputText;

/**
 * The rules an event has to obey, whatever created it.
 * <p>
 * An event reaches the system in two ways: out of a data file, or created by a user (bonus). Both
 * ways check exactly the same rules here, so a user can never create an event that the data file
 * would have been rejected for.
 */
public final class EventValidator {

    private static final int MIN_COMMISSION = 0;
    private static final int MAX_COMMISSION = 90;
    private static final int REQUIRED_OPTIONS = 2;

    private EventValidator() {
    }

    public static void requireName(String name) {
        if (name == null || name.isBlank()) {
            throw UserInputException.emptyField("event name");
        }
        requireEnglish(name);
    }

    /**
     * Any text of an event - its name, its description or an option - has to be written in English.
     */
    public static void requireEnglish(String text) {
        if (!InputText.isEnglish(text)) {
            throw UserInputException.notEnglish();
        }
    }

    public static void requireDescription(String eventName, String description) {
        if (description == null || description.isBlank()) {
            throw UserInputException.emptyField("event description");
        }
        requireEnglish(description);
    }

    public static void requireCommissionInRange(String eventName, int commissionPercent) {
        if (commissionPercent < MIN_COMMISSION || commissionPercent > MAX_COMMISSION) {
            throw EventException.commissionOutOfRange(eventName, commissionPercent);
        }
    }

    public static void requireTwoOptions(int optionCount) {
        if (optionCount != REQUIRED_OPTIONS) {
            throw FileLoadException.wrongOptionCount();
        }
    }

    /**
     * Both options must be named, and the two names must differ (ignoring case).
     */
    public static void requireOptionNames(String eventName, String firstOption,
                                          String secondOption) {
        if (firstOption == null || firstOption.isBlank() || secondOption == null
                || secondOption.isBlank()) {
            throw UserInputException.emptyField("names of both options");
        }
        requireEnglish(firstOption);
        requireEnglish(secondOption);
        if (firstOption.trim().equalsIgnoreCase(secondOption.trim())) {
            throw EventException.sameOptionNames(eventName, firstOption.trim());
        }
    }

    public static void requireLiquidityPositive(String eventName, int liquidity) {
        if (liquidity <= 0) {
            throw EventException.invalidLiquidity(eventName, liquidity);
        }
    }

    public static void requireBaseValuePositive(String eventName, int baseValue) {
        if (baseValue <= 0) {
            throw EventException.baseValueNotPositive(eventName, baseValue);
        }
    }

    /**
     * The initial investment buys whole pairs of shares, so it cannot be negative and has to divide
     * by the base value without a remainder.
     */
    public static void requireInitialInvestment(String eventName, int initialInvestment,
                                                int baseValue) {
        if (initialInvestment < 0) {
            throw EventException.initialInvestmentNegative(eventName, initialInvestment);
        }
        if (initialInvestment % baseValue != 0) {
            throw EventException.initialNotDivisible(eventName, initialInvestment,
                    baseValue);
        }
    }
}
