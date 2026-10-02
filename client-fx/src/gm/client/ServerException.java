package gm.client;

import gm.engine.exception.GuessMarketException;

/**
 * An error the client got from talking to the server: the title and the message the server answered with,
 * or the explanation that the server could not be reached. It is a {@link GuessMarketException}, so the screens
 * of exercise 2 show it with the same dialog as an error of the engine.
 */
public class ServerException extends GuessMarketException {

    private static final long serialVersionUID = 1L;

    public ServerException(String title, String message) {
        super(title, message);
    }

    public ServerException(String title, String message, Throwable cause) {
        super(title, message, cause);
    }
}
