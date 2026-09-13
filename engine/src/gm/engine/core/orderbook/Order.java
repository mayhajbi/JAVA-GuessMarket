package gm.engine.core.orderbook;

import gm.dto.OrderSide;
import gm.engine.core.User;

import java.io.Serializable;

/**
 * A single order in the order book of one option: who placed it, buy or sell, the price per share (in
 * whole cents) and the quantity that is still waiting to be matched.
 */
public class Order implements Serializable {

    private static final long serialVersionUID = 1L;

    private final User owner;
    private final OrderSide side;
    private final int optionIndex;
    private final long priceCents;
    private long remainingQuantity;

    Order(User owner, OrderSide side, int optionIndex, long priceCents, long quantity) {
        this.owner = owner;
        this.side = side;
        this.optionIndex = optionIndex;
        this.priceCents = priceCents;
        this.remainingQuantity = quantity;
    }

    public User getOwner() {
        return owner;
    }

    public OrderSide getSide() {
        return side;
    }

    public int getOptionIndex() {
        return optionIndex;
    }

    public long getPriceCents() {
        return priceCents;
    }

    public long getRemainingQuantity() {
        return remainingQuantity;
    }

    boolean isFilled() {
        return remainingQuantity == 0;
    }

    void fill(long quantity) {
        remainingQuantity -= quantity;
    }
}
