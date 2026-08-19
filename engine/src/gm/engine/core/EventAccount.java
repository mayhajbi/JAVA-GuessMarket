package gm.engine.core;

import java.io.Serializable;

/**
 * The trading account of a single event (the account of its market maker).
 * <p>
 * The balance holds the subsidy that was invested when the event started, the money that was paid
 * by the participants and the commissions that were collected. The prizes of the winners are paid
 * out of it, so it may also end up with a negative balance.
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
     * Registers an amount as commission that was collected by this account. The money itself is
     * either deposited (a commission that is collected on every purchase) or simply kept in the
     * account instead of being paid to the winners (a commission that is collected on close).
     */
    public void addCollectedCommission(double amount) {
        totalCommissionCollected += amount;
    }
}
