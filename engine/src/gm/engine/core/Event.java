package gm.engine.core;

import gm.dto.CommissionType;
import gm.dto.EventStatus;
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
    private final TradingMethod tradingMethod;
    private final EventAccount account;
    private final List<Trade> trades = new ArrayList<>();

    private EventStatus status = EventStatus.ACTIVE;
    private Integer winningOptionIndex;

    public Event(int id,
                 String name,
                 String description,
                 int commissionPercent,
                 CommissionType commissionType,
                 List<EventOption> options,
                 TradingMethod tradingMethod) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.commissionPercent = commissionPercent;
        this.commissionType = commissionType;
        this.options = new ArrayList<>(options);
        this.tradingMethod = tradingMethod;
        this.account = new EventAccount(tradingMethod.initialSubsidy(options.size()));
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

    public EventStatus getStatus() {
        return status;
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
        return tradingMethod.initialSubsidy(options.size());
    }

    /**
     * The current value of a single share of the requested option, between 0 and 1.
     *
     * @param optionIndex zero based index of the option
     */
    public double getOptionValue(int optionIndex) {
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

    private long[] sharesPerOption() {
        long[] shares = new long[options.size()];
        for (int i = 0; i < shares.length; i++) {
            shares[i] = options.get(i).getShares();
        }
        return shares;
    }
}
