package gm.engine.core.method;

import java.io.Serializable;

/**
 * The pricing rules of an event: how much a share is worth right now and how much a purchase costs.
 * <p>
 * An implementation does not hold the amount of shares that were already bought - the shares are
 * held by the event itself and are handed over on every call. This keeps a single source of truth
 * for the state of the event, and lets the system support additional trading methods (such as an
 * order book) later on without changing the event itself.
 */
public interface TradingMethod extends Serializable {

    /**
     * The amount of money the market maker has to invest in the event when it starts, before any
     * share was bought.
     *
     * @param optionCount amount of options the event has
     * @return the initial subsidy of the event
     */
    double initialSubsidy(int optionCount);

    /**
     * The current value of a single share of the requested option, between 0 and 1.
     *
     * @param shares      amount of shares bought so far, per option
     * @param optionIndex zero based index of the requested option
     * @return the current value of one share of that option
     */
    double optionValue(long[] shares, int optionIndex);

    /**
     * The price of buying an additional amount of shares of the requested option.
     *
     * @param shares      amount of shares bought so far, per option
     * @param optionIndex zero based index of the option to buy
     * @param quantity    amount of shares to buy
     * @return the price of the shares themselves, without any commission
     */
    double buyCost(long[] shares, int optionIndex, long quantity);
}
