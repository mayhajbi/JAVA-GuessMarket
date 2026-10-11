// The one message dialog of the application. A dialog that is asked for while another one is open waits
// for it to be closed.
import {el} from './dom.js';

const dialog = document.getElementById('message');
const titleElement = document.getElementById('message-title');
const textElement = document.getElementById('message-text');
const buttons = document.getElementById('message-buttons');
let queue = Promise.resolve();

function show(title, message, withCancel) {
    titleElement.textContent = title;
    textElement.textContent = message;
    buttons.replaceChildren(
        el('button', {type: 'button', autofocus: '', onclick: () => dialog.close('ok')}, 'OK'),
        ...(withCancel ? [el('button', {type: 'button', onclick: () => dialog.close('cancel')}, 'Cancel')] : []));
    return new Promise((resolve) => {
        dialog.addEventListener('close', () => resolve(dialog.returnValue === 'ok'), {once: true});
        dialog.returnValue = '';
        dialog.showModal();
    });
}

function open(title, message, withCancel) {
    const shown = queue.then(() => show(title, message, withCancel));
    queue = shown.catch(() => {});
    return shown;
}

/** Shows the refusal of the server (title and message), or any other failure. */
export function showError(error) {
    return open(error.title ?? 'Unexpected error', error.message, false);
}

/** Reports an action that was completed, or an input that cannot be used. Resolves when closed. */
export function showInformation(title, message) {
    return open(title, message, false);
}

/** @return a promise of whether the user approved */
export function confirm(title, message) {
    return open(title, message, true);
}
