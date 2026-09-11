package gm.dto;

/**
 * The life cycle state of an event. The only legal transitions are INACTIVE to ACTIVE (the market
 * maker opens the event) and ACTIVE to CLOSED (the market maker closes it).
 */
public enum EventStatus {

    INACTIVE("Inactive"),
    ACTIVE("Active"),
    CLOSED("Closed");

    private final String displayName;

    EventStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
