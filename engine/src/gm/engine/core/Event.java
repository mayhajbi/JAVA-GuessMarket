package gm.engine.core;

import gm.dto.CommissionType;
import gm.dto.EventStatus;
import gm.dto.EventType;
import gm.engine.core.method.TradingMethod;
import gm.engine.exception.EventAlreadyOpenedException;
import gm.engine.exception.EventNotActiveException;
import gm.engine.exception.InsufficientFundsException;
import gm.engine.exception.InvalidOptionSelectionException;
import gm.engine.exception.InvalidQuantityException;
import gm.engine.exception.NotEventMarketMakerException;
import gm.engine.exception.UserBlockedException;

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
 * market maker opens it and pays the initial subsidy) to CLOSED (the market maker decides the winning
 * option, the winners are paid and the market maker receives the commission and what is left in the
 * event account).
 */
public class Event implements Serializable {

    private static final long serialVersionUID = 1L;

    /** The amount that is paid at the end of the event for every share of the winning option. */
    private static final double PAYOUT_PER_WINNING_SHARE = 1.0;

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
    private final EventAccount account = new EventAccount(0);
    private final List<Trade> trades = new ArrayList<>();

    /** Order book parameters; all {@code null} for an LMSR event. */
    private final Integer orderBookBaseValue;
    private final Boolean orderBookAllowMint;
    private final Integer orderBookInitialInvestment;

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
                EventType.LMSR, tradingMethod, null, null, null);
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
                EventType.ORDER_BOOK, null, baseValue, allowMint, initialInvestment);
    }

    private Event(int id,
                  String name,
                  String description,
                  int commissionPercent,
                  CommissionType commissionType,
                  List<EventOption> options,
                  EventType type,
                  TradingMethod tradingMethod,
                  Integer orderBookBaseValue,
                  Boolean orderBookAllowMint,
                  Integer orderBookInitialInvestment) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.commissionPercent = commissionPercent;
        this.commissionType = commissionType;
        this.options = new ArrayList<>(options);
        this.type = type;
        this.tradingMethod = tradingMethod;
        this.orderBookBaseValue = orderBookBaseValue;
        this.orderBookAllowMint = orderBookAllowMint;
        this.orderBookInitialInvestment = orderBookInitialInvestment;
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

    /** The base value (d) of an order book event. */
    public int getOrderBookBaseValue() {
        requireOrderBook();
        return orderBookBaseValue;
    }

    /** Whether minting is allowed on an order book event. */
    public boolean isOrderBookMintAllowed() {
        requireOrderBook();
        return orderBookAllowMint;
    }

    /** The amount the market maker invests when an order book event is opened. */
    public int getOrderBookInitialInvestment() {
        requireOrderBook();
        return orderBookInitialInvestment;
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
     * The current value of a single share of the requested option, between 0 and 1.
     *
     * @param optionIndex zero based index of the option
     */
    public double getOptionValue(int optionIndex) {
        requireLmsr();
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
     * Opens the event for trading. The market maker pays the initial subsidy from the own account
     * into the event account. Unlike a purchase, opening must be fully covered: when the balance of
     * the market maker is not enough, nothing happens and the event stays inactive.
     *
     * @param user the user asking to open the event - must be its market maker
     */
    public void open(User user) {
        requireLmsr();
        requireMarketMaker(user, "open");
        if (status != EventStatus.INACTIVE) {
            throw new EventAlreadyOpenedException(id, name, status);
        }
        requireNotBlocked(user, "open the event [" + name + "]");

        double subsidy = getInitialSubsidy();
        double balance = user.getAccount().getBalance();
        if (balance < subsidy) {
            throw new InsufficientFundsException(user.getName(),
                    "open the event [" + name + "] (the initial subsidy)", subsidy, balance);
        }

        user.getAccount().withdraw(subsidy);
        account.deposit(subsidy);
        status = EventStatus.ACTIVE;
    }

    /**
     * Buys shares of one of the options of this event.
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
        requireLmsr();
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
     * Closes the event and decides its winning option.
     * <p>
     * Every holder of the winning option is paid {@value #PAYOUT_PER_WINNING_SHARE} per share out of
     * the event account. When the commission of the event is collected on close, it is taken out of
     * that payment and goes to the market maker. Whatever is left in the event account afterwards
     * (the unused part of the subsidy) is returned to the market maker as well.
     * <p>
     * A blocked market maker may still close the event: deciding the result is not a trading action,
     * and without it the winners could never be paid.
     *
     * @param user               the user asking to close the event - must be its market maker
     * @param winningOptionIndex zero based index of the winning option
     */
    public void close(User user, int winningOptionIndex) {
        requireLmsr();
        requireMarketMaker(user, "close");
        requireActive();
        validateOptionIndex(winningOptionIndex);

        for (Map.Entry<User, Long> holding : holdingsOf(winningOptionIndex).entrySet()) {
            double payout = holding.getValue() * PAYOUT_PER_WINNING_SHARE;
            double commission = commissionType == CommissionType.ON_CLOSE
                    ? payout * commissionPercent / PERCENT
                    : 0;
            account.withdraw(payout);
            holding.getKey().getAccount().deposit(payout - commission);
            if (commission > 0) {
                marketMaker.getAccount().deposit(commission);
                account.addCollectedCommission(commission);
            }
        }

        // LMSR keeps the account at C(q), which is never below the payout, so this is the unused part
        // of the subsidy. A negative balance (not reachable with LMSR) is left as is.
        double leftover = account.getBalance();
        if (leftover > 0) {
            account.withdraw(leftover);
            marketMaker.getAccount().deposit(leftover);
        }

        this.winningOptionIndex = winningOptionIndex;
        this.status = EventStatus.CLOSED;
    }

    /**
     * @return whether the user has traded in this event (participation starts with the first action)
     */
    public boolean hasParticipant(User user) {
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
     * The amount of shares every user holds of one option, in the order the users first bought it.
     */
    private Map<User, Long> holdingsOf(int optionIndex) {
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

    /**
     * Order book trading (opening, pricing, buying, closing) is built in a later step of the exercise.
     * Until then any such call on an order book event fails loudly instead of misbehaving.
     */
    private void requireLmsr() {
        if (type != EventType.LMSR) {
            throw new UnsupportedOperationException("Event [" + name + "] (id " + id + ") uses the "
                    + "order book method, whose trading is not implemented yet.");
        }
    }

    private void requireOrderBook() {
        if (type != EventType.ORDER_BOOK) {
            throw new UnsupportedOperationException("Event [" + name + "] (id " + id + ") is not an "
                    + "order book event.");
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
