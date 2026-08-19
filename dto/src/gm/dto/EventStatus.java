package gm.dto;

/**
 * The life cycle state of an event.
 */
public enum EventStatus {

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
