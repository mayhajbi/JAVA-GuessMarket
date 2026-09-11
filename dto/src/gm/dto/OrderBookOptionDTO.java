package gm.dto;

import java.util.List;

/**
 * The order book of a single option and its price statistics. A statistic is {@code null} when it
 * cannot be calculated yet (for example, there is no bid, or no trade happened).
 *
 * @param optionName name of the option
 * @param bids       the waiting buy orders, best (highest) price first
 * @param asks       the waiting sell orders, best (lowest) price first
 * @param lastPrice  the price of the last trade (LAST)
 * @param bestBid    the highest waiting buy price (BID)
 * @param bestAsk    the lowest waiting sell price (ASK)
 * @param midPrice   the average of BID and ASK (MID)
 * @param spread     ASK minus BID (SPREAD)
 */
public record OrderBookOptionDTO(String optionName,
                                 List<OrderDTO> bids,
                                 List<OrderDTO> asks,
                                 Double lastPrice,
                                 Double bestBid,
                                 Double bestAsk,
                                 Double midPrice,
                                 Double spread) {

    public OrderBookOptionDTO {
        bids = List.copyOf(bids);
        asks = List.copyOf(asks);
    }
}
