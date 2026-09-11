package gm.engine.core.orderbook;

import gm.engine.core.User;

import java.io.Serializable;

/**
 * A single trade in an order book event, from the side of one buyer. A resale creates one trade (the
 * counterparty is the seller). A mint creates two - one for each of the two buyers, each with the
 * other buyer as the counterparty.
 */
public class OrderBookTrade implements Serializable {

    private static final long serialVersionUID = 1L;

    private final User buyer;
    private final User counterparty;
    private final int optionIndex;
    private final long quantity;
    private final long priceCents;
    private final double commission;
    private final boolean minted;

    OrderBookTrade(User buyer, User counterparty, int optionIndex, long quantity, long priceCents,
                   double commission, boolean minted) {
        this.buyer = buyer;
        this.counterparty = counterparty;
        this.optionIndex = optionIndex;
        this.quantity = quantity;
        this.priceCents = priceCents;
        this.commission = commission;
        this.minted = minted;
    }

    public User getBuyer() {
        return buyer;
    }

    public User getCounterparty() {
        return counterparty;
    }

    public int getOptionIndex() {
        return optionIndex;
    }

    public long getQuantity() {
        return quantity;
    }

    public long getPriceCents() {
        return priceCents;
    }

    public double getCommission() {
        return commission;
    }

    public boolean isMinted() {
        return minted;
    }
}
