package gm.engine.core;

import gm.dto.CommissionType;
import gm.dto.EventStatus;
import gm.dto.EventType;
import gm.engine.core.method.TradingMethod;
import gm.engine.exception.EventNotActiveException;
import gm.engine.exception.InvalidOptionSelectionException;
import gm.engine.exception.InvalidQuantityException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * A single event in the system: its details, its options, its trading account and its history.
 * <p>
 * All the money rules of an event live here: the subsidy that is invested when the event is created,
 * the price of a purchase, the commission and the payment to the winners when the event is closed.
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
    private final EventAccount account;
    private final List<Trade> trades = new ArrayList<>();

    /** Order book parameters; all {@code null} for an LMSR event. */
    private final Integer orderBookBaseValue;
    private final Boolean orderBookAllowMint;
    private final Integer orderBookInitialInvestment;

    private EventStatus status = EventStatus.ACTIVE;
    private Integer winningOptionIndex;
    /** The single user allowed to open, fund and close this event; wired in while the file loads. */
    private User marketMaker;

    /**
     * Creates an LMSR event. The initial subsidy of the method is invested into the event account.
     */
    public static Event lmsr(int id,
                             String name,
                             String description,
                             int commissionPercent,
                             CommissionType commissionType,
                             List<EventOption> options,
                             TradingMethod tradingMethod) {
        return new Event(id, name, description, commissionPercent, commissionType, options,
                EventType.LMSR, tradingMethod, tradingMethod.initialSubsidy(options.size()),
                null, null, null);
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
                EventType.ORDER_BOOK, null, 0.0, baseValue, allowMint, initialInvestment);
    }

    private Event(int id,
                  String name,
                  String description,
                  int commissionPercent,
                  CommissionType commissionType,
                  List<EventOption> options,
                  EventType type,
                  TradingMethod tradingMethod,
                  double initialAccountBalance,
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
        this.account = new EventAccount(initialAccountBalance);
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
     * The subsidy that was invested in this event when it was created.
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
     * Buys shares of one of the options of this event.
     * <p>
     * The price of the shares is calculated by the trading method of the event and is deposited into
     * the event account. When the commission of the event is collected on every purchase, it is
     * added on top of that price and is deposited into the event account as well.
     *
     * @param optionIndex zero based index of the option to buy
     * @param quantity    amount of shares to buy, must be positive
     * @return the trade that was created
     */
    public Trade buy(int optionIndex, long quantity) {
        requireLmsr();
        requireActive();
        validateOptionIndex(optionIndex);
        if (quantity <= 0) {
            throw new InvalidQuantityException(quantity);
        }

        double sharesCost = tradingMethod.buyCost(sharesPerOption(), optionIndex, quantity);
        double commission = commissionType == CommissionType.ON_PURCHASE
                ? sharesCost * commissionPercent / PERCENT
                : 0;

        options.get(optionIndex).addShares(quantity);
        account.deposit(sharesCost);
        if (commission > 0) {
            account.deposit(commission);
            account.addCollectedCommission(commission);
        }

        Trade trade = new Trade(optionIndex, quantity, sharesCost, commission);
        trades.add(trade);
        return trade;
    }

    /**
     * Closes the event and decides its winning option.
     * <p>
     * Every share of the winning option is worth {@value #PAYOUT_PER_WINNING_SHARE}. When the
     * commission of the event is collected on close, it is taken out of that total payment and stays
     * in the event account, and the winners are paid the rest. Whatever is left in the account (a
     * positive or a negative balance) stays there.
     *
     * @param winningOptionIndex zero based index of the winning option
     */
    public void close(int winningOptionIndex) {
        requireActive();
        validateOptionIndex(winningOptionIndex);

        double totalPayout = options.get(winningOptionIndex).getShares() * PAYOUT_PER_WINNING_SHARE;
        double commission = commissionType == CommissionType.ON_CLOSE
                ? totalPayout * commissionPercent / PERCENT
                : 0;
        if (commission > 0) {
            account.addCollectedCommission(commission);
        }
        account.withdraw(totalPayout - commission);

        this.winningOptionIndex = winningOptionIndex;
        this.status = EventStatus.CLOSED;
    }

    /**
     * Makes sure the given zero based option index belongs to this event.
     */
    public void validateOptionIndex(int optionIndex) {
        if (optionIndex < 0 || optionIndex >= options.size()) {
            throw new InvalidOptionSelectionException(id, options.size(), optionIndex + 1);
        }
    }

    private void requireActive() {
        if (!isActive()) {
            throw new EventNotActiveException(id, name);
        }
    }

    /**
     * Order book trading (pricing, buying, closing) is built in a later step of the exercise. Until
     * then any such call on an order book event fails loudly instead of misbehaving.
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
