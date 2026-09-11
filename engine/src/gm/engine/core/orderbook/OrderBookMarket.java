package gm.engine.core.orderbook;

import gm.dto.CommissionType;
import gm.dto.OrderSide;
import gm.engine.core.Event;
import gm.engine.core.User;
import gm.engine.exception.InsufficientSharesException;
import gm.engine.exception.InvalidOrderException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The order book trading method of an event: a book of waiting buy orders (bids) and sell orders
 * (asks) for every option, the position of every participant, and the rules that match orders.
 * <p>
 * Prices are kept in whole cents, so the matching - including the mint rule, where two prices have to
 * add up to the base value - is exact. The money of every trade moves right away between the accounts
 * of the users, the event and its market maker, which are reached through the event that is handed
 * over on every call.
 * <p>
 * The events of the system always have exactly two options, which is what makes a mint (one share of
 * each option) possible.
 */
public class OrderBookMarket implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final int CENTS_PER_UNIT = 100;
    private static final double PERCENT = 100.0;
    private static final double WHOLE_CENTS_TOLERANCE = 1e-6;

    private final int baseValue;
    private final boolean mintAllowed;
    private final int initialInvestment;
    private final List<List<Order>> bids = new ArrayList<>();
    private final List<List<Order>> asks = new ArrayList<>();
    private final Long[] lastPriceCents;
    private final Map<User, Position> positions = new LinkedHashMap<>();
    private final List<OrderBookTrade> trades = new ArrayList<>();
    private long nextSequence = 1;

    public OrderBookMarket(int optionCount, int baseValue, boolean mintAllowed, int initialInvestment) {
        this.baseValue = baseValue;
        this.mintAllowed = mintAllowed;
        this.initialInvestment = initialInvestment;
        this.lastPriceCents = new Long[optionCount];
        for (int optionIndex = 0; optionIndex < optionCount; optionIndex++) {
            bids.add(new ArrayList<>());
            asks.add(new ArrayList<>());
        }
    }

    public int getBaseValue() {
        return baseValue;
    }

    public boolean isMintAllowed() {
        return mintAllowed;
    }

    public int getInitialInvestment() {
        return initialInvestment;
    }

    /**
     * The waiting buy orders of an option, best (highest) price first. Orders of blocked users are not
     * included - such a user may not buy anymore, so those orders will never be carried out.
     */
    public List<Order> getBids(int optionIndex) {
        List<Order> activeBids = new ArrayList<>();
        for (Order bid : bids.get(optionIndex)) {
            if (!bid.getOwner().getAccount().isBlocked()) {
                activeBids.add(bid);
            }
        }
        return Collections.unmodifiableList(activeBids);
    }

    /**
     * The waiting sell orders of an option, best (lowest) price first.
     */
    public List<Order> getAsks(int optionIndex) {
        return Collections.unmodifiableList(asks.get(optionIndex));
    }

    /** @return the price of the last trade of the option, or {@code null} before any trade */
    public Long getLastPriceCents(int optionIndex) {
        return lastPriceCents[optionIndex];
    }

    /** @return the highest waiting buy price of the option, or {@code null} when there is none */
    public Long getBestBidCents(int optionIndex) {
        List<Order> activeBids = getBids(optionIndex);
        return activeBids.isEmpty() ? null : activeBids.get(0).getPriceCents();
    }

    /** @return the lowest waiting sell price of the option, or {@code null} when there is none */
    public Long getBestAskCents(int optionIndex) {
        List<Order> optionAsks = asks.get(optionIndex);
        return optionAsks.isEmpty() ? null : optionAsks.get(0).getPriceCents();
    }

    /**
     * The value of a single share of an option. Once the event is closed it is the base value for the
     * winning option and 0 for the other. Before that it is the mid price when both a bid and an ask
     * are waiting (the best estimate of the value of a share), otherwise the price of the last trade.
     *
     * @return the value, or {@code null} when the option has no price at all yet
     */
    public Double getShareValue(int optionIndex, Optional<Integer> winningOptionIndex) {
        if (winningOptionIndex.isPresent()) {
            return winningOptionIndex.get() == optionIndex ? (double) baseValue : 0.0;
        }
        Long bestBid = getBestBidCents(optionIndex);
        Long bestAsk = getBestAskCents(optionIndex);
        if (bestBid != null && bestAsk != null) {
            return (bestBid + bestAsk) / (2.0 * CENTS_PER_UNIT);
        }
        Long lastPrice = lastPriceCents[optionIndex];
        return lastPrice == null ? null : lastPrice / (double) CENTS_PER_UNIT;
    }

    /** Every participant, in the order they joined the event. */
    public Map<User, Position> getPositions() {
        return Collections.unmodifiableMap(positions);
    }

    /** Every trade of the event, in the order they happened. */
    public List<OrderBookTrade> getTrades() {
        return Collections.unmodifiableList(trades);
    }

    /**
     * @return whether the user holds shares of this event or has ever placed an order in it
     */
    public boolean hasParticipant(User user) {
        return positions.containsKey(user);
    }

    /**
     * Gives the market maker one pair of shares (one of each option) for every base value of the
     * initial investment. The money itself is moved by the event.
     */
    public void allocateInitialPairs(User marketMaker) {
        positionOf(marketMaker).addInitialPairs(initialInvestment / baseValue, initialInvestment);
    }

    /**
     * Converts the price of an order into whole cents, making sure it is legal: at least 0.01, at most
     * the base value minus 0.01, and without fractions of a cent.
     */
    public long toPriceCents(Event event, double price) {
        long maxPriceCents = (long) baseValue * CENTS_PER_UNIT - 1;
        long priceCents = Math.round(price * CENTS_PER_UNIT);
        if (Double.isNaN(price) || priceCents < 1 || priceCents > maxPriceCents) {
            throw InvalidOrderException.priceOutOfRange(event.getId(), event.getName(), price, baseValue);
        }
        if (Math.abs(price * CENTS_PER_UNIT - priceCents) > WHOLE_CENTS_TOLERANCE) {
            throw InvalidOrderException.priceNotWholeCents(event.getId(), event.getName(), price);
        }
        return priceCents;
    }

    /**
     * Places an order and matches it right away against the waiting orders. Whatever is not matched
     * waits in the book of its option.
     * <p>
     * A buy order is matched at the best price available to the buyer: an existing share at the price
     * of a waiting sell order, or - when minting is allowed - a newly minted share at the price that
     * completes a waiting buy order of the other option to the base value. A sell order is matched
     * against the waiting buy orders. Every trade happens at the price of the waiting order.
     *
     * @param event the event this order book belongs to (its accounts, commission and market maker)
     */
    public OrderOutcome placeOrder(Event event, User user, OrderSide side, int optionIndex, long quantity,
                                   long priceCents) {
        if (side == OrderSide.SELL) {
            requireAvailableShares(event, user, optionIndex, quantity);
        }
        // Placing an order makes the user a participant, even if the order is never matched.
        positionOf(user);

        Order order = new Order(nextSequence++, user, side, optionIndex, priceCents, quantity);
        int tradesBefore = trades.size();
        if (side == OrderSide.BUY) {
            matchBuyOrder(event, order);
        } else {
            matchSellOrder(event, order);
        }

        // A buyer that became blocked by this order may not buy anymore, so the unmatched rest of the
        // order is dropped instead of waiting in the book.
        long restingQuantity = 0;
        boolean isBlockedBuyer = side == OrderSide.BUY && user.getAccount().isBlocked();
        if (!order.isFilled() && !isBlockedBuyer) {
            insertByPriority(side == OrderSide.BUY ? bids.get(optionIndex) : asks.get(optionIndex), order);
            restingQuantity = order.getRemainingQuantity();
        }
        return new OrderOutcome(trades.subList(tradesBefore, trades.size()),
                quantity - order.getRemainingQuantity(), restingQuantity);
    }

    /**
     * @return the amount of shares every user holds of one option, in the order the users joined
     */
    public Map<User, Long> holdingsOf(int optionIndex) {
        Map<User, Long> holdings = new LinkedHashMap<>();
        for (Map.Entry<User, Position> entry : positions.entrySet()) {
            long shares = entry.getValue().getShares(optionIndex);
            if (shares > 0) {
                holdings.put(entry.getKey(), shares);
            }
        }
        return holdings;
    }

    /**
     * Registers the payout of a winner when the event is closed. The money itself is moved by the event.
     */
    public void recordPayout(User holder, double payout, double commission) {
        positionOf(holder).recordPayout(payout, commission);
    }

    /**
     * Removes every waiting order. Called when the event is closed.
     */
    public void cancelAllOrders() {
        for (int optionIndex = 0; optionIndex < bids.size(); optionIndex++) {
            bids.get(optionIndex).clear();
            asks.get(optionIndex).clear();
        }
    }

    private void matchBuyOrder(Event event, Order order) {
        int optionIndex = order.getOptionIndex();
        long baseValueCents = (long) baseValue * CENTS_PER_UNIT;
        while (!order.isFilled()) {
            Order ask = bestAsk(optionIndex);
            if (ask != null && ask.getPriceCents() > order.getPriceCents()) {
                ask = null;
            }
            Order complementBid = mintAllowed ? bestActiveBid(otherOption(optionIndex)) : null;
            if (complementBid != null && complementBid.getPriceCents() + order.getPriceCents() < baseValueCents) {
                complementBid = null;
            }
            if (ask == null && complementBid == null) {
                return;
            }

            // On an equal price the existing share is sold first - a mint adds shares to the market only
            // when it is actually cheaper for the buyer.
            boolean isResaleCheaper = complementBid == null
                    || (ask != null && ask.getPriceCents() <= baseValueCents - complementBid.getPriceCents());
            if (isResaleCheaper) {
                executeResale(event, order, ask);
            } else {
                executeMint(event, order, complementBid, baseValueCents);
            }
        }
    }

    private void matchSellOrder(Event event, Order order) {
        while (!order.isFilled()) {
            Order bid = bestActiveBid(order.getOptionIndex());
            if (bid == null || bid.getPriceCents() < order.getPriceCents()) {
                return;
            }
            executeResale(event, order, bid);
        }
    }

    /**
     * Existing shares change hands: the buyer pays the seller the price of the waiting order, and the
     * shares move from the seller to the buyer.
     */
    private void executeResale(Event event, Order incoming, Order waiting) {
        Order buyOrder = incoming.getSide() == OrderSide.BUY ? incoming : waiting;
        Order sellOrder = incoming.getSide() == OrderSide.SELL ? incoming : waiting;
        User buyer = buyOrder.getOwner();
        User seller = sellOrder.getOwner();
        int optionIndex = incoming.getOptionIndex();
        long quantity = Math.min(incoming.getRemainingQuantity(), waiting.getRemainingQuantity());
        long priceCents = waiting.getPriceCents();

        double amount = toMoney(priceCents, quantity);
        double commission = purchaseCommission(event, amount);
        buyer.getAccount().withdraw(amount + commission);
        seller.getAccount().deposit(amount);
        payCommission(event, commission);
        positionOf(buyer).recordPurchase(optionIndex, quantity, amount, commission);
        positionOf(seller).recordSale(optionIndex, quantity, amount);

        lastPriceCents[optionIndex] = priceCents;
        trades.add(new OrderBookTrade(buyer, seller, optionIndex, quantity, priceCents, commission, false));
        fill(incoming, waiting, quantity);
    }

    /**
     * New pairs of shares are created for two buyers of opposite options. The waiting order pays its
     * own price, the incoming order pays what completes it to the base value, and all the money goes
     * into the event account.
     */
    private void executeMint(Event event, Order incoming, Order waitingBid, long baseValueCents) {
        User incomingBuyer = incoming.getOwner();
        User waitingBuyer = waitingBid.getOwner();
        int incomingOption = incoming.getOptionIndex();
        int waitingOption = waitingBid.getOptionIndex();
        long quantity = Math.min(incoming.getRemainingQuantity(), waitingBid.getRemainingQuantity());
        long waitingPriceCents = waitingBid.getPriceCents();
        long incomingPriceCents = baseValueCents - waitingPriceCents;

        double incomingAmount = toMoney(incomingPriceCents, quantity);
        double waitingAmount = toMoney(waitingPriceCents, quantity);
        double incomingCommission = purchaseCommission(event, incomingAmount);
        double waitingCommission = purchaseCommission(event, waitingAmount);
        incomingBuyer.getAccount().withdraw(incomingAmount + incomingCommission);
        waitingBuyer.getAccount().withdraw(waitingAmount + waitingCommission);
        event.getAccount().deposit(incomingAmount + waitingAmount);
        payCommission(event, incomingCommission + waitingCommission);
        positionOf(incomingBuyer).recordPurchase(incomingOption, quantity, incomingAmount, incomingCommission);
        positionOf(waitingBuyer).recordPurchase(waitingOption, quantity, waitingAmount, waitingCommission);

        lastPriceCents[incomingOption] = incomingPriceCents;
        lastPriceCents[waitingOption] = waitingPriceCents;
        trades.add(new OrderBookTrade(incomingBuyer, waitingBuyer, incomingOption, quantity,
                incomingPriceCents, incomingCommission, true));
        trades.add(new OrderBookTrade(waitingBuyer, incomingBuyer, waitingOption, quantity,
                waitingPriceCents, waitingCommission, true));
        fill(incoming, waitingBid, quantity);
    }

    private void fill(Order incoming, Order waiting, long quantity) {
        incoming.fill(quantity);
        waiting.fill(quantity);
        if (waiting.isFilled()) {
            List<List<Order>> books = waiting.getSide() == OrderSide.BUY ? bids : asks;
            books.get(waiting.getOptionIndex()).remove(waiting);
        }
    }

    private void requireAvailableShares(Event event, User user, int optionIndex, long quantity) {
        Position position = positions.get(user);
        long held = position == null ? 0 : position.getShares(optionIndex);
        long offered = 0;
        for (Order ask : asks.get(optionIndex)) {
            if (ask.getOwner() == user) {
                offered += ask.getRemainingQuantity();
            }
        }
        long available = held - offered;
        if (quantity > available) {
            throw new InsufficientSharesException(user.getName(), event.getOptionName(optionIndex),
                    event.getName(), quantity, Math.max(0, available));
        }
    }

    private Order bestAsk(int optionIndex) {
        List<Order> optionAsks = asks.get(optionIndex);
        return optionAsks.isEmpty() ? null : optionAsks.get(0);
    }

    /**
     * The best waiting buy order of an option. Orders of users that became blocked are removed on the
     * way, since they can no longer be carried out.
     */
    private Order bestActiveBid(int optionIndex) {
        List<Order> optionBids = bids.get(optionIndex);
        optionBids.removeIf(bid -> bid.getOwner().getAccount().isBlocked());
        return optionBids.isEmpty() ? null : optionBids.get(0);
    }

    /**
     * Price first, then time: a buy order goes before every buy order with a lower price, a sell order
     * before every sell order with a higher price, and after the orders with the same price.
     */
    private void insertByPriority(List<Order> book, Order order) {
        int index = 0;
        while (index < book.size() && !hasBetterPrice(order, book.get(index))) {
            index++;
        }
        book.add(index, order);
    }

    private boolean hasBetterPrice(Order order, Order other) {
        return order.getSide() == OrderSide.BUY
                ? order.getPriceCents() > other.getPriceCents()
                : order.getPriceCents() < other.getPriceCents();
    }

    private double purchaseCommission(Event event, double amount) {
        return event.getCommissionType() == CommissionType.ON_PURCHASE
                ? amount * event.getCommissionPercent() / PERCENT
                : 0;
    }

    private void payCommission(Event event, double commission) {
        if (commission > 0) {
            event.getMarketMaker().getAccount().deposit(commission);
            event.getAccount().addCollectedCommission(commission);
        }
    }

    private Position positionOf(User user) {
        return positions.computeIfAbsent(user, key -> new Position(bids.size()));
    }

    private static int otherOption(int optionIndex) {
        return optionIndex == 0 ? 1 : 0;
    }

    private static double toMoney(long priceCents, long quantity) {
        return priceCents * quantity / (double) CENTS_PER_UNIT;
    }
}
