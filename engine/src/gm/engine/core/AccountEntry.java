package gm.engine.core;

import gm.dto.AccountEntryType;

/**
 * One movement of money in an account: what kind it was, how much came in (positive) or went out
 * (negative), and what the balance was right after it.
 */
public class AccountEntry {

    private final long timeMillis;
    private final AccountEntryType type;
    private final double amount;
    private final double balanceAfter;

    public AccountEntry(long timeMillis, AccountEntryType type, double amount, double balanceAfter) {
        this.timeMillis = timeMillis;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
    }

    public long getTimeMillis() {
        return timeMillis;
    }

    public AccountEntryType getType() {
        return type;
    }

    public double getAmount() {
        return amount;
    }

    public double getBalanceAfter() {
        return balanceAfter;
    }
}
