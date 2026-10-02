package gm.engine.exception;

/**
 * A line a user tried to add to the chat is empty, or is not written in English.
 */
public class InvalidChatLineException extends GuessMarketException {

    private static final long serialVersionUID = 1L;

    private InvalidChatLineException(String message) {
        super(message);
    }

    public static InvalidChatLineException empty() {
        return new InvalidChatLineException("Write a message before sending.");
    }

    public static InvalidChatLineException notEnglish() {
        return new InvalidChatLineException("Messages must use English letters, digits and common "
                + "punctuation only.");
    }
}
