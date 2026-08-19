package gm.dto;

/**
 * The way the commission of an event is collected.
 */
public enum CommissionType {

    ON_PURCHASE("on-purchase"),
    ON_CLOSE("on-close");

    private final String displayName;

    CommissionType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
