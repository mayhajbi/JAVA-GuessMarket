package gm.dto;

/**
 * The kind of a movement of money in the account of a user.
 */
public enum AccountEntryType {

    DEPOSIT("Deposit"),
    EVENT("Event"),
    PAYOUT("Payout"),
    COMMISSION("Commission");

    private final String displayName;

    AccountEntryType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
