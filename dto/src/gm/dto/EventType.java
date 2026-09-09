package gm.dto;

/**
 * The trading method family of an event: the classic LMSR market maker, or an order book.
 */
public enum EventType {

    LMSR("LMSR"),
    ORDER_BOOK("Order Book");

    private final String displayName;

    EventType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
