package gm.client;

/**
 * The fixed values of the client. The client knows the server automatically: the address is
 * written here once and is not asked from the user.
 */
public final class Constants {

    /** The address of the web application of the server, without a trailing slash. */
    public static final String BASE_URL = "http://localhost:8080/guessmarket";

    /** How long a request may wait for the server before it is reported as a failure. */
    public static final int TIMEOUT_SECONDS = 10;

    private Constants() {
    }
}
