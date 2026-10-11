import {el} from './dom.js';

/**
 * A table of rows. A click on a header sorts by that column (a second click reverses it).
 *
 * @param columns     {title, text(row), numeric?, sort?(row)}: the text shown, and the value to sort by
 *                    when it is not the text
 * @param placeholder what is shown while there are no rows
 * @param rowKey      identifies a row, so that a replaced list keeps the selected row selected
 * @param onSelect    makes the rows selectable; called with the selected row, or null when it is gone
 */
export function createTable({columns, placeholder, rowKey, onSelect}) {
    let rows = [];
    let sortColumn = null;
    let ascending = true;
    let selectedKey = null;
    const arrows = columns.map(() => el('span', {class: 'arrow'}));
    const head = el('tr', {}, ...columns.map((column, index) => el('th',
        {class: column.numeric ? 'numeric' : '', onclick: () => sortBy(index)}, column.title, arrows[index])));
    const body = el('tbody');
    const empty = el('div', {class: 'placeholder'}, placeholder);
    const element = el('div', {class: 'table-box'},
        el('table', {class: 'data'}, el('thead', {}, head), body), empty);

    function sortedRows() {
        if (sortColumn === null) {
            return rows;
        }
        const column = columns[sortColumn];
        const valueOf = column.sort ?? column.text;
        const direction = ascending ? 1 : -1;
        return [...rows].sort((a, b) => {
            const first = valueOf(a);
            const second = valueOf(b);
            const order = typeof first === 'number' ? first - second : String(first).localeCompare(String(second));
            return order * direction;
        });
    }

    function sortBy(index) {
        ascending = sortColumn === index ? !ascending : true;
        sortColumn = index;
        arrows.forEach((arrow, position) => {
            arrow.textContent = position === index ? (ascending ? '▲' : '▼') : '';
        });
        render();
    }

    function render() {
        empty.hidden = rows.length > 0;
        body.replaceChildren(...sortedRows().map((row) => el('tr', {
            class: onSelect && rowKey(row) === selectedKey ? 'selected' : '',
            onclick: () => onSelect && select(row)
        }, ...columns.map((column) => el('td', {class: column.numeric ? 'numeric' : ''}, String(column.text(row)))))));
    }

    function select(row) {
        selectedKey = rowKey(row);
        render();
        onSelect(row);
    }

    return {
        element,
        setRows(newRows) {
            rows = newRows;
            const selectedIsGone = onSelect && selectedKey !== null && !rows.some((row) => rowKey(row) === selectedKey);
            if (selectedIsGone) {
                selectedKey = null;
            }
            render();
            if (selectedIsGone) {
                onSelect(null);
            }
        }
    };
}
