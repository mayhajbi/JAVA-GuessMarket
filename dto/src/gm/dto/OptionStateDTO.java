package gm.dto;

/**
 * The current market state of a single option of an event.
 *
 * @param name   the option name
 * @param value  the current value of one share of this option (between 0 and 1)
 * @param shares total amount of shares of this option bought so far
 */
public record OptionStateDTO(String name, double value, long shares) {
}
