// Calls to the Guess Market server. Every refusal becomes a ServerError with the title and the message
// the server sent, so that one place can show it.
const BASE = '/guessmarket';
const UNREACHABLE_MESSAGE = 'The Guess Market server does not answer. Check that Tomcat is running, and try again.';

export class ServerError extends Error {
    constructor(title, message, status) {
        super(message);
        this.title = title;
        this.status = status;
    }
}

async function request(path, options) {
    let response;
    try {
        response = await fetch(BASE + path, options);
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

export async function getJson(path) {
    return (await request(path)).json();
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
