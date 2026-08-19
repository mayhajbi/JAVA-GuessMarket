package gm.ui;

/**
 * The commands of the main menu, in the order they are presented to the user.
 */
public enum MenuCommand {

    LOAD_EVENTS_FILE("Load an events file (XML)"),
    SHOW_EVENTS("Show all the events in the system"),
    SHOW_MARKET_STATE("Show the trading state of an event"),
    PARTICIPATE("Participate in an event (buy shares)"),
    CLOSE_EVENT("Close an event and decide its result"),
    SAVE_SYSTEM_STATE("Save the current state of the system to a file"),
    LOAD_SYSTEM_STATE("Load a saved state of the system from a file"),
    EXIT("Exit");

    private final String displayName;

    MenuCommand(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
