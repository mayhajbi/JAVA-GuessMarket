package gm.engine.core.orderbook;

import java.io.Serializable;

/**
 * What a single user holds in one order book event, and the money the user paid and received in it.
 */
public class Position implements Serializable {

    private static final long serialVersionUID = 1L;

    private final long[] shares;
    private final double[] paidPerOption;
    private double initialInvestmentPaid;
    private double commissionPaid;
    private double received;

    Position(int optionCount) {
        this.shares = new long[optionCount];
        this.paidPerOption = new double[optionCount];
    }

    public long getShares(int optionIndex) {
        return shares[optionIndex];
    }

    /**
     * @return the money paid for shares of the option that were bought in trades, without commission
     */
    public double getPaid(int optionIndex) {
        return paidPerOption[optionIndex];
    }

    public double getInitialInvestmentPaid() {
        return initialInvestmentPaid;
    }

    /**
     * @return every commission paid: on purchases and out of the payout
     */
    public double getCommissionPaid() {
        return commissionPaid;
    }

    /**
     * @return the money received from selling shares and from the payout, before commission (the
     *         commission is counted once, in {@link #getCommissionPaid()})
     */
    public double getReceived() {
        return received;
    }

    /**
     * Everything received minus everything paid. Before the event is closed it does not include the
     * value of the shares that are still held.
     */
    public double getProfitOrLoss() {
        double paid = initialInvestmentPaid + commissionPaid;
        for (double optionPaid : paidPerOption) {
            paid += optionPaid;
        }
        return received - paid;
    }

    void addInitialPairs(long pairs, double amount) {
        for (int optionIndex = 0; optionIndex < shares.length; optionIndex++) {
            shares[optionIndex] += pairs;
        }
        initialInvestmentPaid += amount;
    }

    void recordPurchase(int optionIndex, long quantity, double amount, double commission) {
        shares[optionIndex] += quantity;
        paidPerOption[optionIndex] += amount;
        commissionPaid += commission;
    }

    void recordSale(int optionIndex, long quantity, double amount) {
        shares[optionIndex] -= quantity;
        received += amount;
    }

    void recordPayout(double payout, double commission) {
        received += payout;
        commissionPaid += commission;
    }
}
