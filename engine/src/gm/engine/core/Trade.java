package gm.engine.core;

import java.io.Serializable;

/**
 * A single purchase that was made in an event.
 */
public class Trade implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int optionIndex;
    private final long shares;
    private final double sharesCost;
    private final double commission;

    public Trade(int optionIndex, long shares, double sharesCost, double commission) {
        this.optionIndex = optionIndex;
        this.shares = shares;
        this.sharesCost = sharesCost;
        this.commission = commission;
    }

    public int getOptionIndex() {
        return optionIndex;
    }

    public long getShares() {
        return shares;
    }

    public double getSharesCost() {
        return sharesCost;
    }

    public double getCommission() {
        return commission;
    }

    public double getTotalPaid() {
        return sharesCost + commission;
    }
}
