package gm.engine.exception;

import gm.dto.EventStatus;
import gm.dto.EventType;

/**
 * An event cannot be created with the given details, or cannot be asked to do something right now:
 * a detail that is missing or out of range, an event that is not open or was already opened, a user
 * who is not its market maker, or a trading method that does not allow the action.
 */
public class EventException extends GuessMarketException {

    private static final long serialVersionUID = 1L;

    private EventException(String title, String message) {
        super(title, message);
    }

    public static EventException sameOptionNames(String eventName, String optionName) {
        return new EventException("Duplicate options", "Both options of " + describeEvent(eventName) + " are named '"
                + optionName + "'. Give the options different names. Names are not case-sensitive.");
    }

    public static EventException invalidLiquidity(String eventName, int b) {
        return new EventException("Invalid liquidity", "The liquidity of " + describeEvent(eventName) + " is " + b
                + ". It must be a positive whole number. In a file, it is the element 'b'.");
    }

    public static EventException baseValueNotPositive(String eventName, int d) {
        return new EventException("Invalid base value", "The base value of " + describeEvent(eventName) + " is " + d
                + ". It must be a positive whole number. In a file, it is the attribute 'd'.");
    }

    public static EventException initialInvestmentNegative(String eventName, int initial) {
        return new EventException("Invalid initial investment", "The initial investment of " + describeEvent(eventName)
                + " is " + initial + ". It cannot be negative. In a file, it is the attribute 'initial'.");
    }

    public static EventException initialNotDivisible(String eventName, int initial, int d) {
        return new EventException("Invalid initial investment", "The initial investment of " + describeEvent(eventName)
                + " is " + initial + ", but it must be a multiple of the base value " + d + ". The market maker "
                + "receives one pair of shares for every " + d + ". In a file, these are the attributes "
                + "'initial' and 'd'.");
    }

    public static EventException commissionOutOfRange(String eventName, int value) {
        return new EventException("Invalid commission", "The commission of " + describeEvent(eventName) + " is " + value
                + "%. It must be a whole number from 0 to 90.");
    }

    public static EventException notActive(String eventName, EventStatus status, String marketMakerName) {
        return new EventException("Event is not active",
                status == EventStatus.INACTIVE
                ? describeEvent(eventName) + " is not open yet. Only its market maker, '" + marketMakerName
                        + "', can open it."
                : describeEvent(eventName) + " is closed, so it can no longer be traded or closed.");
    }

    public static EventException alreadyOpened(String eventName) {
        return new EventException("Event already open",
                describeEvent(eventName) + " was already opened, and cannot be opened again.");
    }

    public static EventException notMarketMaker(String eventName, String marketMakerName, String action) {
        return new EventException("Not the market maker", "You cannot " + action + " " + describeEvent(eventName)
                + ". Only its market maker, '" + marketMakerName + "', can do that.");
    }

    public static EventException wrongTradingMethod(String eventName, EventType type, String action) {
        return new EventException("Wrong trading method", describeEvent(eventName) + " uses the " + type.getDisplayName()
                + " method, so you cannot " + action + " in it.");
    }
}
