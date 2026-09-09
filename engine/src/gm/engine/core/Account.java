package gm.engine.core;

import java.io.Serializable;

/**
 * The money account of a single user.
 * <p>
 * The balance starts at the amount the user was given in the data file and changes with the activity
 * of the user. Receiving money (a prize, a commission) is always possible. Spending money is not
 * checked here: an action that would bring the balance below zero is still carried out, but from that
 * moment the account is {@link #isBlocked() blocked} and the user may not start any further action.
 */
public class Account implements Serializable {

    private static final long serialVersionUID = 1L;

    private double balance;
    private boolean blocked;

    public Account(double initialBalance) {
        this.balance = initialBalance;
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
     * Adds money to the account (a prize, a returned subsidy, a commission). Always allowed.
     */
    public void deposit(double amount) {
        balance += amount;
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
    }
}
