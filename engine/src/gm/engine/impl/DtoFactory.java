package gm.engine.impl;

import gm.dto.EventInfoDTO;
import gm.dto.HistoryPointDTO;
import gm.dto.MarketStateDTO;
import gm.dto.OptionStateDTO;
import gm.dto.OrderBookOptionDTO;
import gm.dto.OrderBookParticipantDTO;
import gm.dto.OrderBookStateDTO;
import gm.dto.OrderBookTradeDTO;
import gm.dto.OrderDTO;
import gm.dto.PriceHistoryDTO;
import gm.dto.TradeRecordDTO;
import gm.dto.UserDetailsDTO;
import gm.dto.UserEventDTO;
import gm.dto.UserInfoDTO;
import gm.engine.core.Event;
import gm.engine.core.EventOption;
import gm.engine.core.HistoryPoint;
import gm.engine.core.Trade;
import gm.engine.core.User;
import gm.engine.core.orderbook.Order;
import gm.engine.core.orderbook.OrderBookMarket;
import gm.engine.core.orderbook.OrderBookTrade;
import gm.engine.core.orderbook.Position;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.IntStream;

/**
 * Builds the data transfer objects the engine returns out of its core objects.
 * <p>
 * A new object is created on every request, so a caller never holds a reference to anything that
 * belongs to the engine.
 */
class DtoFactory {

    EventInfoDTO toEventInfo(Event event) {
        return new EventInfoDTO(event.getId(), event.getName(), event.getDescription(),
                event.getCommissionPercent(), event.getCommissionType(),
                mapAll(event.getOptions(), EventOption::getName),
                event.getStatus(), event.getType(), event.getMarketMaker().getName(),
                event.getAccount().getBalance());
    }

    List<EventInfoDTO> toEventInfoList(Iterable<Event> events) {
        return mapAll(events, this::toEventInfo);
    }

    UserInfoDTO toUserInfo(User user) {
        return new UserInfoDTO(user.getName(), user.getAccount().getBalance(),
                user.getAccount().isBlocked());
    }

    UserDetailsDTO toUserDetails(User user, Iterable<Event> events) {
        List<UserEventDTO> userEvents = new ArrayList<>();
        for (Event event : events) {
            boolean marketMaker = event.getMarketMaker() == user;
            boolean participant = event.hasParticipant(user);
            if (marketMaker || participant) {
                userEvents.add(new UserEventDTO(toEventInfo(event), marketMaker, participant));
            }
        }
        return new UserDetailsDTO(user.getName(), user.getAccount().getBalance(),
                user.getAccount().isBlocked(), userEvents);
    }

    List<UserInfoDTO> toUserInfoList(Iterable<User> users) {
        return mapAll(users, this::toUserInfo);
    }

    /**
     * The value of every option of the event over time, one series per option.
     */
    List<PriceHistoryDTO> toPriceHistory(Event event) {
        return perOption(event, index -> new PriceHistoryDTO(event.getOptionName(index),
                toHistoryPoints(event.getPriceHistory(index))));
    }

    /**
     * The balance of the account of the user over time.
     */
    List<HistoryPointDTO> toBalanceHistory(User user) {
        return toHistoryPoints(user.getAccount().getBalanceHistory());
    }

    private List<HistoryPointDTO> toHistoryPoints(List<HistoryPoint> points) {
        return mapAll(points, point -> new HistoryPointDTO(point.getTimeMillis(), point.getValue()));
    }

    MarketStateDTO toMarketState(Event event) {
        return new MarketStateDTO(toEventInfo(event), toOptionStates(event),
                event.getAccount().getBalance(), event.getAccount().getTotalCommissionCollected(),
                toTradeHistory(event), event.getWinningOptionName().orElse(null));
    }

    private List<OptionStateDTO> toOptionStates(Event event) {
        return perOption(event, index -> new OptionStateDTO(event.getOptionName(index),
                event.getOptionValue(index), event.getOptions().get(index).getShares()));
    }

    OrderBookStateDTO toOrderBookState(Event event) {
        OrderBookMarket market = event.getOrderBook();
        List<OrderBookOptionDTO> options = perOption(event, index -> toOrderBookOption(event, market, index));
        List<OrderBookParticipantDTO> participants = mapAll(market.getPositions().entrySet(),
                entry -> toParticipant(event, market, entry.getKey(), entry.getValue()));
        return new OrderBookStateDTO(toEventInfo(event), market.getBaseValue(), market.isMintAllowed(),
                market.getInitialInvestment(), options, participants,
                toOrderBookTrades(event, latestFirst(market.getTrades())),
                event.getAccount().getTotalCommissionCollected(), event.getWinningOptionName().orElse(null));
    }

    List<OrderBookTradeDTO> toOrderBookTrades(Event event, List<OrderBookTrade> trades) {
        return mapAll(trades, trade -> new OrderBookTradeDTO(trade.getBuyer().getName(),
                trade.getCounterparty().getName(), event.getOptionName(trade.getOptionIndex()),
                trade.getQuantity(), OrderBookMarket.toPrice(trade.getPriceCents()),
                trade.getCommission(), trade.isMinted()));
    }

    private OrderBookOptionDTO toOrderBookOption(Event event, OrderBookMarket market, int optionIndex) {
        Long bestBid = market.getBestBidCents(optionIndex);
        Long bestAsk = market.getBestAskCents(optionIndex);
        Long spreadCents = bestBid == null || bestAsk == null ? null : bestAsk - bestBid;
        return new OrderBookOptionDTO(event.getOptionName(optionIndex),
                toOrders(market.getBids(optionIndex)), toOrders(market.getAsks(optionIndex)),
                OrderBookMarket.toPrice(market.getLastPriceCents(optionIndex)),
                OrderBookMarket.toPrice(bestBid), OrderBookMarket.toPrice(bestAsk),
                market.getMidPrice(optionIndex), OrderBookMarket.toPrice(spreadCents));
    }

    private List<OrderDTO> toOrders(List<Order> orders) {
        return mapAll(orders, order -> new OrderDTO(order.getOwner().getName(), order.getRemainingQuantity(),
                OrderBookMarket.toPrice(order.getPriceCents())));
    }

    private OrderBookParticipantDTO toParticipant(Event event, OrderBookMarket market, User user,
                                                  Position position) {
        List<Long> shares = new ArrayList<>();
        List<Double> holdingValues = new ArrayList<>();
        List<Double> paid = new ArrayList<>();
        for (int index = 0; index < event.getOptionCount(); index++) {
            long held = position.getShares(index);
            Double shareValue = market.getShareValue(index, event.getWinningOptionIndex());
            shares.add(held);
            holdingValues.add(shareValue == null ? null : shareValue * held);
            paid.add(position.getPaid(index));
        }
        return new OrderBookParticipantDTO(user.getName(), shares, holdingValues, paid,
                position.getInitialInvestmentPaid(), position.getCommissionPaid(), position.getReceived(),
                position.getProfitOrLoss());
    }

    /**
     * The trading history of the event, ordered from the latest trade to the first one.
     */
    private List<TradeRecordDTO> toTradeHistory(Event event) {
        return mapAll(latestFirst(event.getTrades()), trade -> new TradeRecordDTO(trade.getBuyer().getName(),
                event.getOptionName(trade.getOptionIndex()), trade.getShares(), trade.getSharesCost(),
                trade.getCommission(), trade.getTotalPaid()));
    }

    /**
     * @return a copy of the items (kept in the order they happened) from the latest to the first
     */
    private static <T> List<T> latestFirst(List<T> items) {
        List<T> reversed = new ArrayList<>(items);
        Collections.reverse(reversed);
        return reversed;
    }

    /**
     * Converts every item into a data transfer object, keeping their order.
     */
    private static <S, T> List<T> mapAll(Iterable<S> items, Function<S, T> converter) {
        List<T> converted = new ArrayList<>();
        for (S item : items) {
            converted.add(converter.apply(item));
        }
        return converted;
    }

    /**
     * Builds one data transfer object for every option of the event, by its zero based index.
     */
    private static <T> List<T> perOption(Event event, IntFunction<T> converter) {
        return IntStream.range(0, event.getOptionCount()).mapToObj(converter).toList();
    }
}
