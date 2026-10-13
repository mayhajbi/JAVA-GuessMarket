// The one message dialog of the application. A dialog that is asked for while another one is open waits
// for it to be closed.
import {el} from './dom.js';

const dialog = document.getElementById('message');
const titleElement = document.getElementById('message-title');
const textElement = document.getElementById('message-text');
const extraElement = document.getElementById('message-extra');
const buttons = document.getElementById('message-buttons');
let queue = Promise.resolve();

function show(title, message, withCancel, extra) {
    titleElement.textContent = title;
    textElement.textContent = message;
    extraElement.replaceChildren(...(extra ? [extra] : []));
    // Enter acts on the button that has the focus: a dialog that asks to approve something starts with the
    // focus on Cancel, so that a stray Enter never approves; with an input, the focus starts in the input.
    const focusCancel = withCancel && !extra;
    buttons.replaceChildren(
        el('button', {type: 'button', autofocus: focusCancel || extra ? null : '', onclick: () => dialog.close('ok')}, 'OK'),
        ...(withCancel ? [el('button', {type: 'button', autofocus: focusCancel ? '' : null,
            onclick: () => dialog.close('cancel')}, 'Cancel')] : []));
    return new Promise((resolve) => {
        dialog.addEventListener('close', () => resolve(dialog.returnValue === 'ok'), {once: true});
        dialog.returnValue = '';
        dialog.showModal();
    });
}

function open(title, message, withCancel, extra) {
    const shown = queue.then(() => show(title, message, withCancel, extra));
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

/**
 * Enter in the input is the same as OK. The key press is consumed here: left alone, it would go on to
 * whatever gets the focus once the dialog closes (the button that opened it), and act on it as well.
 *
 * @return a promise of the text the user typed, or null when the user cancelled
 */
export function askText(title, label) {
    const input = el('input', {autofocus: ''});
    input.addEventListener('keydown', (event) => {
        if (event.key === 'Enter') {
            event.preventDefault();
            dialog.close('ok');
        }
    });
    return open(title, label, true, input).then((approved) => approved ? input.value : null);
}
