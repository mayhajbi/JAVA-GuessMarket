// The fixed values of the client.

/** How long a request of the user (an action) may wait for the server before it is reported as a failure. */
export const TIMEOUT_SECONDS = 10;

/**
 * How long an automatic update may wait. It is shorter than TIMEOUT_SECONDS: a server that is up but does
 * not answer is reported after MAX_FAILURES updates. It stays far above the usual time of an answer.
 */
export const REFRESH_TIMEOUT_SECONDS = 5;

/** How often, in milliseconds, the screen that is shown pulls its data from the server. */
export const REFRESH_RATE = 500;

/** After this many updates in a row got no answer, the server is reported as not reachable. */
export const MAX_FAILURES = 3;
