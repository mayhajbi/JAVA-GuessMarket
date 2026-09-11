package gm.engine.impl;

import gm.dto.EventInfoDTO;
import gm.dto.MarketStateDTO;
import gm.dto.OptionStateDTO;
import gm.dto.OrderBookOptionDTO;
import gm.dto.OrderBookParticipantDTO;
import gm.dto.OrderBookStateDTO;
import gm.dto.OrderBookTradeDTO;
import gm.dto.OrderDTO;
import gm.dto.TradeRecordDTO;
import gm.dto.UserDetailsDTO;
import gm.dto.UserEventDTO;
import gm.dto.UserInfoDTO;
import gm.engine.core.Event;
import gm.engine.core.EventOption;
import gm.engine.core.Trade;
import gm.engine.core.User;
import gm.engine.core.orderbook.Order;
import gm.engine.core.orderbook.OrderBookMarket;
import gm.engine.core.orderbook.OrderBookTrade;
import gm.engine.core.orderbook.Position;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Builds the data transfer objects the engine returns out of its core objects.
 * <p>
 * A new object is created on every request, so a caller never holds a reference to anything that
 * belongs to the engine.
 */
class DtoFactory {

    EventInfoDTO toEventInfo(Event event) {
        List<String> optionNames = new ArrayList<>();
        for (EventOption option : event.getOptions()) {
            optionNames.add(option.getName());
        }
        return new EventInfoDTO(event.getId(), event.getName(), event.getDescription(),
                event.getCommissionPercent(), event.getCommissionType(), optionNames,
                event.getStatus(), event.getType(), event.getMarketMaker().getName(),
                event.getAccount().getBalance());
    }

    List<EventInfoDTO> toEventInfoList(Iterable<Event> events) {
        List<EventInfoDTO> eventInfoList = new ArrayList<>();
        for (Event event : events) {
            eventInfoList.add(toEventInfo(event));
        }
        return eventInfoList;
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
        List<UserInfoDTO> userInfoList = new ArrayList<>();
        for (User user : users) {
            userInfoList.add(toUserInfo(user));
        }
        return userInfoList;
    }

    MarketStateDTO toMarketState(Event event) {
        return new MarketStateDTO(toEventInfo(event), toOptionStates(event),
                event.getAccount().getBalance(), event.getAccount().getTotalCommissionCollected(),
                toTradeHistory(event), event.getWinningOptionName().orElse(null));
    }

    private List<OptionStateDTO> toOptionStates(Event event) {
        List<OptionStateDTO> optionStates = new ArrayList<>();
        for (int index = 0; index < event.getOptionCount(); index++) {
            EventOption option = event.getOptions().get(index);
            optionStates.add(new OptionStateDTO(option.getName(), event.getOptionValue(index),
                    option.getShares()));
        }
        return optionStates;
    }

    OrderBookStateDTO toOrderBookState(Event event) {
        OrderBookMarket market = event.getOrderBook();
        List<OrderBookOptionDTO> options = new ArrayList<>();
        for (int index = 0; index < event.getOptionCount(); index++) {
            options.add(toOrderBookOption(event, market, index));
        }
        List<OrderBookParticipantDTO> participants = new ArrayList<>();
        for (Map.Entry<User, Position> entry : market.getPositions().entrySet()) {
            participants.add(toParticipant(event, market, entry.getKey(), entry.getValue()));
        }
        List<OrderBookTrade> trades = new ArrayList<>(market.getTrades());
        Collections.reverse(trades);
        return new OrderBookStateDTO(toEventInfo(event), market.getBaseValue(), market.isMintAllowed(),
                market.getInitialInvestment(), options, participants, toOrderBookTrades(event, trades),
                event.getAccount().getTotalCommissionCollected(), event.getWinningOptionName().orElse(null));
    }

    List<OrderBookTradeDTO> toOrderBookTrades(Event event, List<OrderBookTrade> trades) {
        List<OrderBookTradeDTO> tradeDtos = new ArrayList<>();
        for (OrderBookTrade trade : trades) {
            tradeDtos.add(new OrderBookTradeDTO(trade.getBuyer().getName(),
                    trade.getCounterparty().getName(), event.getOptionName(trade.getOptionIndex()),
                    trade.getQuantity(), toPrice(trade.getPriceCents()), trade.getCommission(),
                    trade.isMinted()));
        }
        return tradeDtos;
    }

    private OrderBookOptionDTO toOrderBookOption(Event event, OrderBookMarket market, int optionIndex) {
        Long bestBid = market.getBestBidCents(optionIndex);
        Long bestAsk = market.getBestAskCents(optionIndex);
        boolean isQuoted = bestBid != null && bestAsk != null;
        return new OrderBookOptionDTO(event.getOptionName(optionIndex),
                toOrders(market.getBids(optionIndex)), toOrders(market.getAsks(optionIndex)),
                toPrice(market.getLastPriceCents(optionIndex)), toPrice(bestBid), toPrice(bestAsk),
                isQuoted ? (bestBid + bestAsk) / 200.0 : null,
                isQuoted ? toPrice(bestAsk - bestBid) : null);
    }

    private List<OrderDTO> toOrders(List<Order> orders) {
        List<OrderDTO> orderDtos = new ArrayList<>();
        for (Order order : orders) {
            orderDtos.add(new OrderDTO(order.getOwner().getName(), order.getRemainingQuantity(),
                    toPrice(order.getPriceCents())));
        }
        return orderDtos;
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

    private static Double toPrice(Long priceCents) {
        return priceCents == null ? null : priceCents / 100.0;
    }

    /**
     * The trading history of the event, ordered from the latest trade to the first one.
     */
    private List<TradeRecordDTO> toTradeHistory(Event event) {
        List<Trade> trades = event.getTrades();
        List<TradeRecordDTO> history = new ArrayList<>();
        for (int index = trades.size() - 1; index >= 0; index--) {
            Trade trade = trades.get(index);
            history.add(new TradeRecordDTO(trade.getBuyer().getName(),
                    event.getOptionName(trade.getOptionIndex()),
                    trade.getShares(), trade.getSharesCost(), trade.getCommission(),
                    trade.getTotalPaid()));
        }
        return history;
    }
}
