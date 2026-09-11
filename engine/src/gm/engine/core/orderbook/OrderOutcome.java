package gm.engine.core.orderbook;

import java.util.List;

/**
 * The result of placing a single order.
 *
 * @param trades          the trades the order created right away, in the order they happened
 * @param filledQuantity  amount of shares of the order that were matched
 * @param restingQuantity amount of shares of the order that now wait in the order book
 */
public record OrderOutcome(List<OrderBookTrade> trades, long filledQuantity, long restingQuantity) {

    public OrderOutcome {
        trades = List.copyOf(trades);
    }
}
