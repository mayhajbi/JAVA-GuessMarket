package gm.dto;

/**
 * A single value of something that is followed over time, and the moment it was recorded.
 *
 * @param timeMillis the moment the value was recorded, as the system clock reported it
 * @param value      the value at that moment
 */
public record HistoryPointDTO(long timeMillis, double value) {
}
