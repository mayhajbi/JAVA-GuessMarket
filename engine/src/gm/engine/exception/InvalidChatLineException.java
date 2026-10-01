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
        return new InvalidChatLineException("The chat line is empty. Please write a message before sending it.");
    }

    public static InvalidChatLineException notEnglish() {
        return new InvalidChatLineException("A chat line has to be written in English. " + USE_ENGLISH);
    }
}
