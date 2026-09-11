package gm.engine.core;

import java.io.Serializable;

/**
 * A single purchase that was made in an event, and the user who made it.
 */
public class Trade implements Serializable {

    private static final long serialVersionUID = 1L;

    private final User buyer;
    private final int optionIndex;
    private final long shares;
    private final double sharesCost;
    private final double commission;

    public Trade(User buyer, int optionIndex, long shares, double sharesCost, double commission) {
        this.buyer = buyer;
        this.optionIndex = optionIndex;
        this.shares = shares;
        this.sharesCost = sharesCost;
        this.commission = commission;
    }

    public User getBuyer() {
        return buyer;
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
