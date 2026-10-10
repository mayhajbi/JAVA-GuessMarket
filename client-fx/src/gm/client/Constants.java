package gm.client;

/**
 * The fixed values of the client. The client knows the server automatically: the address is
 * written here once and is not asked from the user.
 */
public final class Constants {

    /** The address of the web application of the server, without a trailing slash. */
    public static final String BASE_URL = "http://localhost:8080/guessmarket";

    /**
     * How long a request of the user (an action, an upload) may wait for the server before it is reported
     * as a failure.
     */
    public static final int TIMEOUT_SECONDS = 10;

    /**
     * How long an automatic update may wait for the server. It is shorter than {@link #TIMEOUT_SECONDS}:
     * a server that is up but does not answer is reported after {@link #MAX_FAILURES} updates, which is
     * {@code MAX_FAILURES * REFRESH_TIMEOUT_SECONDS} seconds. It stays far above the usual time of an answer
     * (milliseconds on the same machine), so that a busy server is not mistaken for a stuck one.
     */
    public static final int REFRESH_TIMEOUT_SECONDS = 5;

    /** How often, in milliseconds, every refreshed part of the screen pulls its data from the server. */
    public static final int REFRESH_RATE = 500;

    /** After this many refresh requests in a row got no answer, the server is reported as not reachable. */
    public static final int MAX_FAILURES = 3;

    private Constants() {
    }
}
