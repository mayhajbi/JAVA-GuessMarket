package gm.engine.exception;

/**
 * A line a user tried to add to the chat is empty, or is not written in English.
 */
public class InvalidChatLineException extends GuessMarketException {

    private static final long serialVersionUID = 1L;

    private InvalidChatLineException(String title, String message) {
        super(title, message);
    }

    public static InvalidChatLineException empty() {
        return new InvalidChatLineException("Empty message", "Write a message before sending.");
    }

    public static InvalidChatLineException notEnglish() {
        return new InvalidChatLineException("English only", "Messages must use English letters, digits and common "
                + "punctuation only.");
    }
}
