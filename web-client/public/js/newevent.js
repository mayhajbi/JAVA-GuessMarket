import {postJson} from './api.js';
import {showError, showInformation} from './dialog.js';
import {el} from './dom.js';
import {COMMISSION_TYPES} from './format.js';
import {readWholeNumber} from './inputs.js';

const HEADER = 'New event';
const EMPTY_FIELD_CLASS = 'field-error';

function labelled(name, control) {
    return el('label', {class: 'form-row'}, el('span', {class: 'field-name'}, name), control);
}

function hint(text) {
    return el('div', {class: 'placeholder'}, text);
}

/**
 * Opens the form of a new event. It closes only once the server accepted the event, so a refused event
 * keeps whatever was already typed. The server checks every value; the form only spares a rejected request
 * for an empty field, or for a text that is not a number at all.
 *
 * @return a promise of the created event (as in the list of events), or null when the form was cancelled
 */
export function openNewEventForm() {
    const name = el('input', {placeholder: 'Enter a clear title for the event'});
    const description = el('textarea', {rows: '2', placeholder: 'Describe the event for other users: what it is '
        + 'about, and how the winning option will be decided, etc.'});
    const firstOption = el('input', {placeholder: 'Enter an option.'});
    const secondOption = el('input', {placeholder: 'Enter a different option'});
    const commission = el('input', {placeholder: '0 - 90', size: '6'});
    const commissionType = el('select', {}, ...COMMISSION_TYPES.map((type) => el('option', {value: type.value}, type.label)));
    const lmsr = el('input', {type: 'radio', name: 'method', checked: ''});
    const orderBook = el('input', {type: 'radio', name: 'method'});
    const liquidity = el('input', {placeholder: '100', size: '8'});
    const baseValue = el('input', {placeholder: '1', size: '8'});
    const initialInvestment = el('input', {placeholder: '100', size: '8'});
    const allowMint = el('input', {type: 'checkbox'});
    const lmsrFields = el('div', {class: 'actions form-group'}, el('div', {class: 'section-title'}, 'LMSR'),
        labelled('Liquidity', liquidity),
        hint('Enter a positive whole number. The higher it is, the more slowly prices change.'));
    const orderBookFields = el('div', {class: 'actions form-group'}, el('div', {class: 'section-title'}, 'Order Book'),
        labelled('Base value', baseValue),
        hint('Enter what a winning share pays. It is also the highest price an order can have.'),
        labelled('Initial investment', initialInvestment),
        hint('Enter the amount you pay when the event opens. It must be a multiple of the base value.'),
        el('label', {}, allowMint, ' Allow minting new pairs of shares'));
    const dialog = el('dialog', {class: 'new-event'}, el('h2', {}, HEADER),
        labelled('Name', name), labelled('Description', description),
        labelled('Option 1', firstOption), labelled('Option 2', secondOption),
        labelled('Commission', el('span', {}, commission, ' % ', commissionType)),
        el('div', {}, el('span', {class: 'field-name'}, 'Trading method '),
            el('label', {}, lmsr, ' LMSR '), el('label', {}, orderBook, ' Order Book')),
        lmsrFields, orderBookFields);
    const showMethodFields = () => {
        lmsrFields.hidden = !lmsr.checked;
        orderBookFields.hidden = lmsr.checked;
    };
    lmsr.addEventListener('change', showMethodFields);
    orderBook.addEventListener('change', showMethodFields);
    showMethodFields();
    for (const field of [name, description, firstOption, secondOption, commission, liquidity, baseValue, initialInvestment]) {
        field.addEventListener('input', () => field.classList.remove(EMPTY_FIELD_CLASS));
    }

    function markEmptyFields() {
        const required = [name, description, firstOption, secondOption, commission,
            ...(lmsr.checked ? [liquidity] : [baseValue, initialInvestment])];
        const empty = required.filter((field) => field.value.trim() === '');
        empty.forEach((field) => field.classList.add(EMPTY_FIELD_CLASS));
        empty[0]?.focus();
        return empty.length > 0;
    }

    function readNumber(field, what) {
        const text = field.value.trim();
        const value = readWholeNumber(text);
        if (value === null) {
            showInformation(HEADER, `'${text}' is not a valid value for ${what}. Enter a whole number, for example 10.`);
            field.focus();
        }
        return value;
    }

    /** @return the body of the request, or null after telling the user what is missing */
    function toRequest() {
        if (markEmptyFields()) {
            showInformation('Incomplete form', 'Fill in all the empty fields to create a new event.');
            return null;
        }
        const percent = readNumber(commission, 'the commission');
        if (percent === null) {
            return null;
        }
        const request = {name: name.value, description: description.value, commissionPercent: percent,
            commissionType: commissionType.value, firstOption: firstOption.value, secondOption: secondOption.value,
            type: lmsr.checked ? 'LMSR' : 'ORDER_BOOK'};
        if (lmsr.checked) {
            const value = readNumber(liquidity, 'the liquidity value');
            return value === null ? null : {...request, liquidity: value};
        }
        const base = readNumber(baseValue, 'the base value');
        const initial = base === null ? null : readNumber(initialInvestment, 'the initial investment');
        return initial === null ? null : {...request, baseValue: base, allowMint: allowMint.checked,
            initialInvestment: initial};
    }

    return new Promise((resolve) => {
        let created = null;
        const create = async () => {
            const request = toRequest();
            if (request === null) {
                return;
            }
            try {
                created = await postJson('/event/create', request);
                dialog.close();
            } catch (error) {
                showError(error);
            }
        };
        dialog.append(el('div', {class: 'buttons'},
            el('button', {type: 'button', onclick: create}, 'OK'),
            el('button', {type: 'button', onclick: () => dialog.close()}, 'Cancel')));
        dialog.addEventListener('close', () => {
            dialog.remove();
            resolve(created);
        });
        document.body.append(dialog);
        dialog.showModal();
    });
}
