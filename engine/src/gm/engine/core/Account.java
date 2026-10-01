package gm.engine.core;

import gm.dto.AccountEntryType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The money account of a single user.
 * <p>
 * The balance starts at the amount the user was given and changes with the activity of the user.
 * Receiving money (a prize, a commission, a deposit) is always possible. Spending money is not
 * checked here: an action that would bring the balance below zero is still carried out, but while
 * the balance stays below zero the account is {@link #isBlocked() blocked} and the user may not start
 * any further action. Money that comes in again lifts the block once the balance is back to zero.
 * <p>
 * Every movement of money in the system passes through this class, so the account keeps every
 * {@link #getEntries() entry} of its life, and the {@link #getBalanceHistory() history} of the
 * balance is derived from them.
 */
public class Account {

    private final HistoryPoint openingBalance;
    private final List<AccountEntry> entries = new ArrayList<>();

    private double balance;

    public Account(double initialBalance) {
        this.balance = initialBalance;
        this.openingBalance = new HistoryPoint(System.currentTimeMillis(), initialBalance);
    }

    public double getBalance() {
        return balance;
    }

    /**
     * @return whether the balance is below zero right now, which blocks the user from starting any
     *         further action
     */
    public boolean isBlocked() {
        return balance < 0;
    }

    /**
     * Every movement of money since the account was opened, in the order they happened.
     */
    public List<AccountEntry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    /**
     * The balance of the account over time: the amount it started with, and one point for every
     * movement since then, in the order they happened.
     */
    public List<HistoryPoint> getBalanceHistory() {
        List<HistoryPoint> history = new ArrayList<>();
        history.add(openingBalance);
        for (AccountEntry entry : entries) {
            history.add(new HistoryPoint(entry.getTimeMillis(), entry.getBalanceAfter()));
        }
        return history;
    }

    /**
     * Adds money to the account (a prize, a returned subsidy, a commission, a deposit). Always allowed.
     */
    public void deposit(double amount, AccountEntryType type) {
        balance += amount;
        record(type, amount);
    }

    /**
     * Takes money out of the account. The withdrawal is always carried out; if it leaves the balance
     * negative the account is blocked.
     */
    public void withdraw(double amount, AccountEntryType type) {
        balance -= amount;
        record(type, -amount);
    }

    private void record(AccountEntryType type, double signedAmount) {
        entries.add(new AccountEntry(System.currentTimeMillis(), type, signedAmount, balance));
    }
}
