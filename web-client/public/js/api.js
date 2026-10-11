// Calls to the Guess Market server. Every refusal becomes a ServerError with the title and the message
// the server sent, so that one place can show it.
import {TIMEOUT_SECONDS} from './constants.js';

const BASE = '/guessmarket';
const UNREACHABLE_STATUSES = [0, 502];
const UNREACHABLE_MESSAGE = 'The Guess Market server does not answer. Check that Tomcat is running, and try again.';

export class ServerError extends Error {
    constructor(title, message, status) {
        super(message);
        this.title = title;
        this.status = status;
    }
}

async function request(path, options, timeoutSeconds = TIMEOUT_SECONDS) {
    let response;
    try {
        response = await fetch(BASE + path, {...options, signal: AbortSignal.timeout(timeoutSeconds * 1000)});
    } catch (error) {
        throw new ServerError('Server not reachable', UNREACHABLE_MESSAGE, 0);
    }
    if (!response.ok) {
        let body = null;
        try {
            body = await response.json();
        } catch (error) {
            // an answer that is not JSON: the general message below
        }
        throw new ServerError(body?.title ?? 'Request refused',
            body?.message ?? 'The server refused the request. Try again.', response.status);
    }
    return response;
}

/** @param timeoutSeconds how long to wait for the answer; an automatic update waits less than an action */
export async function getJson(path, timeoutSeconds) {
    return (await request(path, undefined, timeoutSeconds)).json();
}

function postOptions(body) {
    return body === undefined
        ? {method: 'POST'}
        : {method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify(body)};
}

/** A POST whose answer has no content (the details, when there are any, go as the JSON body). */
export async function post(path, body) {
    await request(path, postOptions(body));
}

/** A POST whose answer is JSON. */
export async function postJson(path, body) {
    return (await request(path, postOptions(body))).json();
}

/** @return whether the error is that the server did not answer (the proxy of the client answers 502) */
export function isUnreachable(error) {
    return error instanceof ServerError && UNREACHABLE_STATUSES.includes(error.status);
}
