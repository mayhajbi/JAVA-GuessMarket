package gm.engine.core.method;

/**
 * The Logarithmic Market Scoring Rule (LMSR) trading method.
 * <p>
 * The value of an option is
 * <pre>   p_i = e^(q_i / b) / sum( e^(q_j / b) )</pre>
 * and the amount of money in the event account after a series of purchases is
 * <pre>   C(q) = b * ln( sum( e^(q_j / b) ) )</pre>
 * The price of a purchase is therefore the difference between the cost function after the purchase
 * and the cost function before it.
 * <p>
 * Both formulas are calculated with the "shift by the maximum exponent" technique, so that large
 * amounts of shares cannot overflow the exponent calculation.
 */
public class LmsrTradingMethod implements TradingMethod {

    private static final long serialVersionUID = 1L;

    private final int liquidity;

    public LmsrTradingMethod(int liquidity) {
        this.liquidity = liquidity;
    }

    @Override
    public double initialSubsidy(int optionCount) {
        return cost(new long[optionCount]);
    }

    @Override
    public double optionValue(long[] shares, int optionIndex) {
        double maxExponent = maxExponent(shares);
        double sum = sumOfShiftedExp(shares, maxExponent);
        return Math.exp(shares[optionIndex] / (double) liquidity - maxExponent) / sum;
    }

    @Override
    public double buyCost(long[] shares, int optionIndex, long quantity) {
        long[] sharesAfterPurchase = shares.clone();
        sharesAfterPurchase[optionIndex] += quantity;
        return cost(sharesAfterPurchase) - cost(shares);
    }

    /**
     * The LMSR cost function: the total amount of money in the event account for the given shares.
     */
    private double cost(long[] shares) {
        double maxExponent = maxExponent(shares);
        double sum = sumOfShiftedExp(shares, maxExponent);
        return liquidity * (maxExponent + Math.log(sum));
    }

    private double maxExponent(long[] shares) {
        double max = Double.NEGATIVE_INFINITY;
        for (long optionShares : shares) {
            max = Math.max(max, optionShares / (double) liquidity);
        }
        return max;
    }

    /**
     * Sum of e^(shares_i / liquidity - maxExponent) over all options, shared by both {@link #cost}
     * and {@link #optionValue} so the "shift by the maximum exponent" technique lives in one place.
     */
    private double sumOfShiftedExp(long[] shares, double maxExponent) {
        double sum = 0;
        for (long optionShares : shares) {
            sum += Math.exp(optionShares / (double) liquidity - maxExponent);
        }
        return sum;
    }
}
