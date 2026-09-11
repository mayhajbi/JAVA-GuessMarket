package gm.engine.core;

import java.io.Serializable;

/**
 * The trading account of a single event.
 * <p>
 * The balance holds the subsidy the market maker paid when opening the event and the money the
 * participants paid for their shares. The prizes of the winners are paid out of it when the event is
 * closed, and whatever is left goes back to the market maker. Commissions are paid straight to the
 * market maker - this account only keeps track of their total.
 */
public class EventAccount implements Serializable {

    private static final long serialVersionUID = 1L;

    private double balance;
    private double totalCommissionCollected;

    public EventAccount(double initialBalance) {
        this.balance = initialBalance;
    }

    public double getBalance() {
        return balance;
    }

    public double getTotalCommissionCollected() {
        return totalCommissionCollected;
    }

    public void deposit(double amount) {
        balance += amount;
    }

    public void withdraw(double amount) {
        balance -= amount;
    }

    /**
     * Registers an amount of commission the market maker collected from this event. The money itself
     * is paid to the market maker, not into this account.
     */
    public void addCollectedCommission(double amount) {
        totalCommissionCollected += amount;
    }
}
