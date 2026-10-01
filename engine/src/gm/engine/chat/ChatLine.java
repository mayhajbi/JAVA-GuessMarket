package gm.engine.chat;

/**
 * A single line of the chat: who wrote it, when, and what it says.
 */
public class ChatLine {

    private final String userName;
    private final long timeMillis;
    private final String text;

    public ChatLine(String userName, String text) {
        this.userName = userName;
        this.timeMillis = System.currentTimeMillis();
        this.text = text;
    }

    public String getUserName() {
        return userName;
    }

    public long getTimeMillis() {
        return timeMillis;
    }

    public String getText() {
        return text;
    }
}
