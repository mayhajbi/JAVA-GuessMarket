package gm.dto;

import java.util.List;

/**
 * The result of placing a single order.
 *
 * @param trades          the trades the order created right away, in the order they happened
 * @param filledQuantity  amount of shares of the order that were matched
 * @param restingQuantity amount of shares of the order that now wait in the order book (0 when the
 *                        order was fully matched, or when its rest was dropped)
 * @param userBalance     the balance of the user right after the order
 * @param userBlocked     whether the user is blocked from further actions - true when this order (or
 *                        an earlier action) brought the balance below zero
 * @param stateAfterOrder the state of the event right after the order
 */
public record OrderResultDTO(List<OrderBookTradeDTO> trades,
                             long filledQuantity,
                             long restingQuantity,
                             double userBalance,
                             boolean userBlocked,
                             OrderBookStateDTO stateAfterOrder) {

    public OrderResultDTO {
        trades = List.copyOf(trades);
    }
}
