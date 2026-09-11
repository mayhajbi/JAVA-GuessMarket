package gm.engine.core;

import gm.dto.CommissionType;
import gm.dto.EventStatus;
import gm.dto.EventType;
import gm.dto.OrderSide;
import gm.engine.core.method.TradingMethod;
import gm.engine.core.orderbook.OrderBookMarket;
import gm.engine.core.orderbook.OrderOutcome;
import gm.engine.exception.EventAlreadyOpenedException;
import gm.engine.exception.EventNotActiveException;
import gm.engine.exception.InsufficientFundsException;
import gm.engine.exception.InvalidOptionSelectionException;
import gm.engine.exception.InvalidOrderException;
import gm.engine.exception.InvalidQuantityException;
import gm.engine.exception.NotEventMarketMakerException;
import gm.engine.exception.UserBlockedException;
import gm.engine.exception.WrongTradingMethodException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A single event in the system: its details, its options, its trading account and its history.
 * <p>
 * All the money rules of an event live here. The life cycle is INACTIVE (as loaded) to ACTIVE (the
 * market maker opens it and pays the initial subsidy or investment) to CLOSED (the market maker
 * decides the winning option, the winners are paid and the market maker receives the commission and
 * what is left in the event account).
 * <p>
 * An LMSR event is priced by its {@link TradingMethod} and shares are bought directly. An order book
 * event is traded through the orders of its {@link OrderBookMarket}.
 */
public class Event implements Serializable {

    private static final long serialVersionUID = 1L;

    /** The amount that is paid at the end of an LMSR event for every share of the winning option. */
    private static final double LMSR_PAYOUT_PER_WINNING_SHARE = 1.0;

    private static final double PERCENT = 100.0;

    private final int       id;
    private final String name;
    private final String description;
    private final int commissionPercent;
    private final CommissionType commissionType;
    private final List<EventOption> options;
    private final EventType type;
    /** The pricing rules of an LMSR event; {@code null} for an order book event. */
    private final TradingMethod tradingMethod;
    /** The order books of an order book event; {@code null} for an LMSR event. */
    private final OrderBookMarket orderBook;
    private final EventAccount account = new EventAccount(0);
    /** The purchases of an LMSR event (an order book event keeps its trades in its order book). */
    private final List<Trade> trades = new ArrayList<>();

    private EventStatus status = EventStatus.INACTIVE;
    private Integer winningOptionIndex;
    /** The single user allowed to open, fund and close this event; wired in while the file loads. */
    private User marketMaker;

    /**
     * Creates an LMSR event. Its account starts empty - the market maker pays the initial subsidy of
     * the method when the event is opened.
     */
    public static Event lmsr(int id,
                             String name,
                             String description,
                             int commissionPercent,
                             CommissionType commissionType,
                             List<EventOption> options,
                             TradingMethod tradingMethod) {
        return new Event(id, name, description, commissionPercent, commissionType, options,
                EventType.LMSR, tradingMethod, null);
    }

    /**
     * Creates an order book event. Its account starts empty - the market maker pays the initial
     * investment when the event is opened.
     *
     * @param baseValue         the base value (d): a positive integer
     * @param allowMint          whether minting new share pairs is allowed on this event
     * @param initialInvestment the amount the market maker invests when the event is opened
     */
    public static Event orderBook(int id,
                                  String name,
                                  String description,
                                  int commissionPercent,
                                  CommissionType commissionType,
                                  List<EventOption> options,
                                  int baseValue,
                                  boolean allowMint,
                                  int initialInvestment) {
        return new Event(id, name, description, commissionPercent, commissionType, options,
                EventType.ORDER_BOOK, null,
                new OrderBookMarket(options.size(), baseValue, allowMint, initialInvestment));
    }

    private Event(int id,
                  String name,
                  String description,
                  int commissionPercent,
                  CommissionType commissionType,
                  List<EventOption> options,
                  EventType type,
                  TradingMethod tradingMethod,
                  OrderBookMarket orderBook) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.commissionPercent = commissionPercent;
        this.commissionType = commissionType;
        this.options = new ArrayList<>(options);
        this.type = type;
        this.tradingMethod = tradingMethod;
        this.orderBook = orderBook;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getCommissionPercent() {
        return commissionPercent;
    }

    public CommissionType getCommissionType() {
        return commissionType;
    }

    public EventType getType() {
        return type;
    }

    public EventStatus getStatus() {
        return status;
    }

    public User getMarketMaker() {
        return marketMaker;
    }

    /**
     * Wires the market maker of this event. Called once, while the data file is loaded, after the
     * users of the file are known.
     */
    public void setMarketMaker(User marketMaker) {
        this.marketMaker = marketMaker;
    }

    /**
     * @return the order books of an order book event
     */
    public OrderBookMarket getOrderBook() {
        requireOrderBook("show order books");
        return orderBook;
    }

    public boolean isActive() {
        return status == EventStatus.ACTIVE;
    }

    public EventAccount getAccount() {
        return account;
    }

    public List<EventOption> getOptions() {
        return Collections.unmodifiableList(options);
    }

    public List<Trade> getTrades() {
        return Collections.unmodifiableList(trades);
    }

    public int getOptionCount() {
        return options.size();
    }

    /**
     * The subsidy the market maker of an LMSR event pays into the event account when opening it (0
     * for an order book event).
     */
    public double getInitialSubsidy() {
        return type == EventType.LMSR ? tradingMethod.initialSubsidy(options.size()) : 0;
    }

    /**
     * The current value of a single share of the requested option of an LMSR event, between 0 and 1.
     *
     * @param optionIndex zero based index of the option
     */
    public double getOptionValue(int optionIndex) {
        requireLmsr("show LMSR option values");
        validateOptionIndex(optionIndex);
        return tradingMethod.optionValue(sharesPerOption(), optionIndex);
    }

    public String getOptionName(int optionIndex) {
        validateOptionIndex(optionIndex);
        return options.get(optionIndex).getName();
    }

    public Optional<Integer> getWinningOptionIndex() {
        return Optional.ofNullable(winningOptionIndex);
    }

    public Optional<String> getWinningOptionName() {
        return getWinningOptionIndex().map(index -> options.get(index).getName());
    }

    /**
     * Opens the event for trading. The market maker pays from the own account into the event account:
     * the initial subsidy of an LMSR event, or the initial investment of an order book event - which
     * also gives the market maker the initial pairs of shares. Unlike a trade, opening must be fully
     * covered: when the balance of the market maker is not enough, nothing happens and the event stays
     * inactive.
     *
     * @param user the user asking to open the event - must be its market maker
     */
    public void open(User user) {
        requireMarketMaker(user, "open");
        if (status != EventStatus.INACTIVE) {
            throw new EventAlreadyOpenedException(id, name, status);
        }
        requireNotBlocked(user, "open the event [" + name + "]");

        boolean isLmsr = type == EventType.LMSR;
        double required = isLmsr ? getInitialSubsidy() : orderBook.getInitialInvestment();
        double balance = user.getAccount().getBalance();
        if (balance < required) {
            throw new InsufficientFundsException(user.getName(), "open the event [" + name + "] ("
                    + (isLmsr ? "the initial subsidy" : "the initial investment") + ")", required, balance);
        }

        user.getAccount().withdraw(required);
        account.deposit(required);
        if (!isLmsr) {
            orderBook.allocateInitialPairs(user);
        }
        status = EventStatus.ACTIVE;
    }

    /**
     * Buys shares of one of the options of an LMSR event.
     * <p>
     * The buyer pays the price of the shares, calculated by the trading method, into the event
     * account. When the commission of the event is collected on every purchase, the buyer pays it on
     * top of that price, straight to the market maker. The purchase is carried out even if it brings
     * the balance of the buyer below zero - that blocks the buyer from any further action.
     *
     * @param buyer       the user who buys the shares
     * @param optionIndex zero based index of the option to buy
     * @param quantity    amount of shares to buy, must be positive
     * @return the trade that was created
     */
    public Trade buy(User buyer, int optionIndex, long quantity) {
        requireLmsr("buy shares directly");
        requireActive();
        requireNotBlocked(buyer, "buy shares");
        validateOptionIndex(optionIndex);
        if (quantity <= 0) {
            throw new InvalidQuantityException(quantity);
        }

        double sharesCost = tradingMethod.buyCost(sharesPerOption(), optionIndex, quantity);
        double commission = commissionType == CommissionType.ON_PURCHASE
                ? sharesCost * commissionPercent / PERCENT
                : 0;

        options.get(optionIndex).addShares(quantity);
        buyer.getAccount().withdraw(sharesCost + commission);
        account.deposit(sharesCost);
        if (commission > 0) {
            marketMaker.getAccount().deposit(commission);
            account.addCollectedCommission(commission);
        }

        Trade trade = new Trade(buyer, optionIndex, quantity, sharesCost, commission);
        trades.add(trade);
        return trade;
    }

    /**
     * Places an order in the order book of one option of an order book event, and matches it right
     * away (see {@link OrderBookMarket#placeOrder}). Like a purchase, a trade is carried out even if it
     * brings the balance of a buyer below zero, which blocks that buyer.
     *
     * @param user        the user who places the order
     * @param side        buy or sell
     * @param optionIndex zero based index of the option
     * @param quantity    amount of shares, must be positive
     * @param price       price per share, in whole cents, between 0.01 and d - 0.01
     */
    public OrderOutcome placeOrder(User user, OrderSide side, int optionIndex, long quantity, double price) {
        requireOrderBook("place orders");
        requireActive();
        requireNotBlocked(user, "place orders");
        validateOptionIndex(optionIndex);
        if (side == null) {
            throw InvalidOrderException.missingSide(id, name);
        }
        if (quantity <= 0) {
            throw new InvalidQuantityException(quantity);
        }
        long priceCents = orderBook.toPriceCents(this, price);
        return orderBook.placeOrder(this, user, side, optionIndex, quantity, priceCents);
    }

    /**
     * Closes the event and decides its winning option.
     * <p>
     * Every holder of the winning option is paid out of the event account - 1 per share in an LMSR
     * event, the base value (d) per share in an order book event. When the commission of the event is
     * collected on close, it is taken out of that payment and goes to the market maker. Whatever is
     * left in the event account afterwards (the unused part of an LMSR subsidy) is returned to the
     * market maker as well. The waiting orders of an order book event are cancelled.
     * <p>
     * A blocked market maker may still close the event: deciding the result is not a trading action,
     * and without it the winners could never be paid.
     *
     * @param user               the user asking to close the event - must be its market maker
     * @param winningOptionIndex zero based index of the winning option
     */
    public void close(User user, int winningOptionIndex) {
        requireMarketMaker(user, "close");
        requireActive();
        validateOptionIndex(winningOptionIndex);

        boolean isLmsr = type == EventType.LMSR;
        double payoutPerShare = isLmsr ? LMSR_PAYOUT_PER_WINNING_SHARE : orderBook.getBaseValue();
        Map<User, Long> holdings = isLmsr
                ? lmsrHoldingsOf(winningOptionIndex)
                : orderBook.holdingsOf(winningOptionIndex);
        for (Map.Entry<User, Long> holding : holdings.entrySet()) {
            User holder = holding.getKey();
            double payout = holding.getValue() * payoutPerShare;
            double commission = commissionType == CommissionType.ON_CLOSE
                    ? payout * commissionPercent / PERCENT
                    : 0;
            account.withdraw(payout);
            holder.getAccount().deposit(payout - commission);
            if (commission > 0) {
                marketMaker.getAccount().deposit(commission);
                account.addCollectedCommission(commission);
            }
            if (!isLmsr) {
                orderBook.recordPayout(holder, payout, commission);
            }
        }
        if (!isLmsr) {
            orderBook.cancelAllOrders();
        }

        // LMSR keeps the account at C(q) and an order book keeps d per pair of shares, so the account
        // never falls below the payout and this is the unused part of the money. A negative balance
        // (not reachable with these methods) is left as is.
        double leftover = account.getBalance();
        if (leftover > 0) {
            account.withdraw(leftover);
            marketMaker.getAccount().deposit(leftover);
        }

        this.winningOptionIndex = winningOptionIndex;
        this.status = EventStatus.CLOSED;
    }

    /**
     * @return whether the user takes part in this event (participation starts with the first action)
     */
    public boolean hasParticipant(User user) {
        if (type == EventType.ORDER_BOOK) {
            return orderBook.hasParticipant(user);
        }
        for (Trade trade : trades) {
            if (trade.getBuyer() == user) {
                return true;
            }
        }
        return false;
    }

    /**
     * Makes sure the given zero based option index belongs to this event.
     */
    public void validateOptionIndex(int optionIndex) {
        if (optionIndex < 0 || optionIndex >= options.size()) {
            throw new InvalidOptionSelectionException(id, options.size(), optionIndex + 1);
        }
    }

    /**
     * The amount of shares every user holds of one option of an LMSR event, in the order the users
     * first bought it.
     */
    private Map<User, Long> lmsrHoldingsOf(int optionIndex) {
        Map<User, Long> holdings = new LinkedHashMap<>();
        for (Trade trade : trades) {
            if (trade.getOptionIndex() == optionIndex) {
                holdings.merge(trade.getBuyer(), trade.getShares(), Long::sum);
            }
        }
        return holdings;
    }

    private void requireActive() {
        if (!isActive()) {
            throw new EventNotActiveException(id, name, status, marketMaker.getName());
        }
    }

    private void requireMarketMaker(User user, String action) {
        if (user != marketMaker) {
            throw new NotEventMarketMakerException(id, name, marketMaker.getName(), user.getName(),
                    action);
        }
    }

    private void requireNotBlocked(User user, String action) {
        if (user.getAccount().isBlocked()) {
            throw new UserBlockedException(user.getName(), action);
        }
    }

    private void requireLmsr(String action) {
        if (type != EventType.LMSR) {
            throw new WrongTradingMethodException(id, name, type, action);
        }
    }

    private void requireOrderBook(String action) {
        if (type != EventType.ORDER_BOOK) {
            throw new WrongTradingMethodException(id, name, type, action);
        }
    }

    private long[] sharesPerOption() {
        long[] shares = new long[options.size()];
        for (int i = 0; i < shares.length; i++) {
            shares[i] = options.get(i).getShares();
        }
        return shares;
    }
}
