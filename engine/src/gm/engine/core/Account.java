package gm.engine.core;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The money account of a single user.
 * <p>
 * The balance starts at the amount the user was given in the data file and changes with the activity
 * of the user. Receiving money (a prize, a commission) is always possible. Spending money is not
 * checked here: an action that would bring the balance below zero is still carried out, but from that
 * moment the account is {@link #isBlocked() blocked} and the user may not start any further action.
 * <p>
 * Every movement of money in the system passes through this class, so the account also keeps the
 * whole {@link #getBalanceHistory() history} of its balance.
 */
public class Account implements Serializable {

    private static final long serialVersionUID = 1L;

    private final List<HistoryPoint> balanceHistory = new ArrayList<>();

    private double balance;
    private boolean blocked;

    public Account(double initialBalance) {
        this.balance = initialBalance;
        recordBalance();
    }

    public double getBalance() {
        return balance;
    }

    /**
     * @return whether the balance has dropped below zero at some point, which blocks the user from
     *         starting any further action
     */
    public boolean isBlocked() {
        return blocked;
    }

    /**
     * The balance of the account over time: the amount it started with, and one point for every
     * change since then, in the order they happened.
     */
    public List<HistoryPoint> getBalanceHistory() {
        return Collections.unmodifiableList(balanceHistory);
    }

    /**
     * Adds money to the account (a prize, a returned subsidy, a commission). Always allowed.
     */
    public void deposit(double amount) {
        balance += amount;
        recordBalance();
    }

    /**
     * Takes money out of the account. The withdrawal is always carried out; if it leaves the balance
     * negative the account becomes blocked.
     */
    public void withdraw(double amount) {
        balance -= amount;
        if (balance < 0) {
            blocked = true;
        }
        recordBalance();
    }

    private void recordBalance() {
        balanceHistory.add(new HistoryPoint(System.currentTimeMillis(), balance));
    }
}
