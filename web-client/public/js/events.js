import {getJson} from './api.js';
import {showError, showInformation} from './dialog.js';
import {el} from './dom.js';
import {createEventDetail} from './eventdetail.js';
import {createFilterGroup} from './filters.js';
import {openNewEventForm} from './newevent.js';
import {
    COMMISSION_TYPES, EVENT_STATUSES, EVENT_TYPES, commission, decimal, displayName
} from './format.js';
import {createTable} from './table.js';

/**
 * The events screen: the events of the system, filtered by type, status and commission method (the
 * filtering itself is done by the server), and the details of the selected event.
 */
export function createEventsScreen(onChanged) {
    const detail = createEventDetail(onChanged);
    const typeFilter = createFilterGroup(EVENT_TYPES, load);
    const statusFilter = createFilterGroup(EVENT_STATUSES, load);
    const commissionFilter = createFilterGroup(COMMISSION_TYPES, load);
    const countLabel = el('div', {class: 'field-name'});
    const table = createTable({
        columns: [
            {title: '#', text: (event) => event.id, numeric: true, sort: (event) => event.id},
            {title: 'Name', text: (event) => event.name},
            {title: 'Status', text: (event) => displayName(EVENT_STATUSES, event.status)},
            {title: 'Type', text: (event) => displayName(EVENT_TYPES, event.type)},
            {title: 'Commission', text: (event) => commission(event.commissionPercent, event.commissionType),
                sort: (event) => event.commissionPercent},
            {title: 'Market maker', text: (event) => event.marketMakerName},
            {title: 'Account', text: (event) => decimal(event.accountBalance), numeric: true,
                sort: (event) => event.accountBalance}
        ],
        placeholder: 'No events to show. Upload a file or change the filters.',
        rowKey: (event) => event.id,
        onSelect: (event) => detail.show(event)
    });
    let latestLoad = 0;

    async function load() {
        const thisLoad = ++latestLoad;
        const query = `types=${typeFilter.selected()}&statuses=${statusFilter.selected()}`
            + `&commissions=${commissionFilter.selected()}`;
        try {
            const [all, visible] = await Promise.all([getJson('/events'), getJson(`/events?${query}`)]);
            if (thisLoad === latestLoad) {
                table.setRows(visible);
                countLabel.textContent = `Showing ${visible.length} of ${all.length} events`;
            }
        } catch (error) {
            showError(error);
        }
    }

    // The user who is logged in is read again with the events: the balance and the block change with the actions.
    async function refresh() {
        try {
            detail.setUser(await getJson('/account'));
        } catch (error) {
            showError(error);
            return;
        }
        await load();
        await detail.refresh();
    }

    async function onNewEvent() {
        const created = await openNewEventForm();
        if (created !== null) {
            await onChanged();
            table.selectKey(created.id);
            showInformation('Event created', `'${created.name}' was created. It is inactive until you open it, `
                + 'and then trading begins.');
        }
    }

    refresh();
    const element = el('div', {class: 'split'},
        el('div', {class: 'events-pane'},
            el('div', {class: 'events-title'}, el('span', {class: 'section-title'}, 'Events'),
                el('button', {type: 'button', onclick: onNewEvent}, 'New event')),
            el('div', {class: 'filters'},
                el('span', {class: 'field-name'}, 'Type'), typeFilter.element,
                el('span', {class: 'field-name'}, 'Status'), statusFilter.element,
                el('span', {class: 'field-name'}, 'Commission'), commissionFilter.element),
            table.element, countLabel),
        detail.element);
    return {element, refresh};
}
