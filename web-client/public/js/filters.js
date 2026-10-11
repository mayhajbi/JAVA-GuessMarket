import {el} from './dom.js';

/**
 * A row of toggle buttons that filters by one property: an "All" button and one button per value. Every
 * value starts selected. "All" selects or clears every value, and it is selected exactly when every
 * value is.
 *
 * @param values   [{value, label}]
 * @param onChange called after the selection changed
 */
export function createFilterGroup(values, onChange) {
    const allButton = el('button', {type: 'button', class: 'toggle selected', onclick: () => {
        const select = !allButton.classList.contains('selected');
        allButton.classList.toggle('selected', select);
        valueButtons.forEach((button) => button.classList.toggle('selected', select));
        onChange();
    }}, 'All');
    const valueButtons = values.map((entry) => el('button', {type: 'button', class: 'toggle selected', onclick: (event) => {
        event.currentTarget.classList.toggle('selected');
        allButton.classList.toggle('selected', valueButtons.every((button) => button.classList.contains('selected')));
        onChange();
    }}, entry.label));

    return {
        element: el('div', {class: 'filter-group'}, allButton, ...valueButtons),
        selected: () => values.filter((entry, index) => valueButtons[index].classList.contains('selected'))
            .map((entry) => entry.value)
    };
}
