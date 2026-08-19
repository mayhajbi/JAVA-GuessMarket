package gm.engine.core;

import java.io.Serializable;

/**
 * A single possible outcome of an event, together with the amount of shares bought from it.
 */
public class EventOption implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String name;
    private long shares;

    public EventOption(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public long getShares() {
        return shares;
    }

    public void addShares(long amount) {
        shares += amount;
    }
}
