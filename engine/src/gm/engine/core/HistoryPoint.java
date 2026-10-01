package gm.engine.core;

/**
 * A single value of something that is followed over time: the value of an option of an event, or the
 * balance of a user account.
 * <p>
 * The point is recorded at the moment the value changed, so a series of points describes the whole
 * way that value went through since it was first known.
 */
public class HistoryPoint {

    private final long timeMillis;
    private final double value;

    public HistoryPoint(long timeMillis, double value) {
        this.timeMillis = timeMillis;
        this.value = value;
    }

    /**
     * @return the moment the value was recorded, as the system clock reported it
     */
    public long getTimeMillis() {
        return timeMillis;
    }

    public double getValue() {
        return value;
    }
}
