package gm.engine.core;

import gm.engine.exception.InvalidCommissionException;
import gm.engine.exception.InvalidEventDetailsException;
import gm.engine.exception.InvalidLiquidityException;
import gm.engine.exception.InvalidOptionsException;
import gm.engine.exception.InvalidOrderBookException;

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
            throw InvalidEventDetailsException.missingName();
        }
    }

    public static void requireDescription(String eventName, String description) {
        if (description == null || description.isBlank()) {
            throw InvalidEventDetailsException.missingDescription(eventName);
        }
    }

    public static void requireCommissionInRange(int id, String eventName, int commissionPercent) {
        if (commissionPercent < MIN_COMMISSION || commissionPercent > MAX_COMMISSION) {
            throw InvalidCommissionException.outOfRange(id, eventName, commissionPercent);
        }
    }

    public static void requireTwoOptions(int id, String eventName, int optionCount) {
        if (optionCount != REQUIRED_OPTIONS) {
            throw new InvalidOptionsException(id, eventName, optionCount);
        }
    }

    public static void requireOptionNames(String eventName, String firstOption, String secondOption) {
        if (firstOption == null || firstOption.isBlank() || secondOption == null
                || secondOption.isBlank()) {
            throw InvalidEventDetailsException.missingOptionName(eventName);
        }
        if (firstOption.trim().equalsIgnoreCase(secondOption.trim())) {
            throw InvalidEventDetailsException.sameOptionNames(eventName, firstOption.trim());
        }
    }

    public static void requireLiquidityPositive(int id, String eventName, int liquidity) {
        if (liquidity <= 0) {
            throw new InvalidLiquidityException(id, eventName, liquidity);
        }
    }

    public static void requireBaseValuePositive(int id, String eventName, int baseValue) {
        if (baseValue <= 0) {
            throw InvalidOrderBookException.baseValueNotPositive(id, eventName, baseValue);
        }
    }

    /**
     * The initial investment buys whole pairs of shares, so it cannot be negative and has to divide
     * by the base value without a remainder.
     */
    public static void requireInitialInvestment(int id, String eventName, int initialInvestment,
                                                int baseValue) {
        if (initialInvestment < 0) {
            throw InvalidOrderBookException.initialInvestmentNegative(id, eventName, initialInvestment);
        }
        if (initialInvestment % baseValue != 0) {
            throw InvalidOrderBookException.initialNotDivisible(id, eventName, initialInvestment,
                    baseValue);
        }
    }
}
