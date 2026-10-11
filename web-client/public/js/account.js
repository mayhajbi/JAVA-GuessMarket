import {getJson, post} from './api.js';
import {createPriceChart} from './chart.js';
import {askText, showError, showInformation} from './dialog.js';
import {el} from './dom.js';
import {createEventDetail} from './eventdetail.js';
import {EVENT_STATUSES, EVENT_TYPES, decimal, displayName} from './format.js';
import {readDecimal} from './inputs.js';
import {createTable} from './table.js';

const ENTRY_TYPES = [
    {value: 'DEPOSIT', label: 'Deposit'}, {value: 'EVENT', label: 'Event'},
    {value: 'PAYOUT', label: 'Payout'}, {value: 'COMMISSION', label: 'Commission'}];
const BALANCE_LABELS = {x: 'Seconds since the first change', y: 'Balance'};
const LOAD_FUNDS = 'Load funds';

function describeRole(row) {
    if (row.marketMaker && row.participant) {
        return 'Market maker, participant';
    }
    return row.marketMaker ? 'Market maker' : 'Participant';
}

/**
 * The account screen: the balance of the user who is logged in and how it changed, the other users, the
 * events the user is connected to (as market maker or participant) and the details of one of them. The
 * user can also load funds.
 *
 * @param onChanged called after an action changed the data in the server; refreshes every screen
 * @return {element, refresh(timeoutSeconds)}: refresh fails with the error of the server
 */
export function createAccountScreen(onChanged) {
    const detail = createEventDetail(onChanged);
    const usersTable = createTable({
        columns: [
            {title: 'Name', text: (user) => user.name},
            {title: 'Balance', text: (user) => decimal(user.balance), numeric: true, sort: (user) => user.balance},
            {title: 'Market maker', text: (user) => user.marketMaker ? 'Yes' : 'No'}
        ],
        placeholder: 'No other users yet.'
    });
    const entriesTable = createTable({
        columns: [
            {title: 'Type', text: (entry) => displayName(ENTRY_TYPES, entry.type)},
            {title: 'Amount', text: (entry) => decimal(entry.amount), numeric: true, sort: (entry) => entry.amount},
            {title: 'Balance', text: (entry) => decimal(entry.balanceAfter), numeric: true,
                sort: (entry) => entry.balanceAfter}
        ],
        placeholder: 'No account activity yet.'
    });
    const eventsTable = createTable({
        columns: [
            {title: 'Event', text: (row) => row.event.name},
            {title: 'Status', text: (row) => displayName(EVENT_STATUSES, row.event.status)},
            {title: 'Type', text: (row) => displayName(EVENT_TYPES, row.event.type)},
            {title: 'Role', text: describeRole}
        ],
        placeholder: 'You are not part of any event yet.',
        rowKey: (row) => row.event.id,
        onSelect: (row) => detail.show(row === null ? null : row.event)
    });
    const nameLabel = el('span', {class: 'section-title'});
    const blockedLabel = el('span', {class: 'blocked'}, 'Blocked - balance below zero');
    const balanceLabel = el('span', {class: 'section-title'});
    const balanceChart = el('div');
    let shownHistory = null;

    async function loadFunds() {
        const text = await askText(LOAD_FUNDS, 'Amount:');
        if (text === null) {
            return;
        }
        const amount = readDecimal(text.trim());
        if (amount === null) {
            showInformation(LOAD_FUNDS, `'${text.trim()}' is not a valid amount. Enter a number, for example 100.`);
            return;
        }
        try {
            await post(`/account/deposit?amount=${amount}`);
            onChanged();
        } catch (error) {
            showError(error);
        }
    }

    async function refresh(timeoutSeconds) {
        const [account, users, entries, history] = await Promise.all(['/account', '/users', '/account/log',
            '/account/history'].map((path) => getJson(path, timeoutSeconds)));
        detail.setUser(account);
        nameLabel.textContent = account.name;
        balanceLabel.textContent = decimal(account.balance);
        blockedLabel.hidden = !account.blocked;
        eventsTable.setRows(account.events);
        usersTable.setRows(users);
        entriesTable.setRows(entries);
        // The graph is drawn again only when the history changed.
        const historyKey = JSON.stringify(history);
        if (historyKey !== shownHistory) {
            shownHistory = historyKey;
            balanceChart.replaceChildren(createPriceChart([{optionName: 'Balance', points: history}], BALANCE_LABELS)
                ?? el('div', {class: 'placeholder'}, 'No balance changes yet.'));
        }
        await detail.refresh(timeoutSeconds);
    }

    refresh().catch(showError);
    const element = el('div', {class: 'split'},
        el('div', {class: 'events-pane'},
            el('div', {class: 'section-title'}, 'Users'), usersTable.element,
            el('div', {class: 'section-title'}, 'Account details'), entriesTable.element,
            el('div', {class: 'balance-row'}, nameLabel, blockedLabel, el('span', {class: 'spacer'}),
                el('span', {class: 'field-name'}, 'Account balance'), balanceLabel,
                el('button', {type: 'button', onclick: loadFunds}, LOAD_FUNDS)),
            balanceChart),
        el('div', {class: 'events-pane'},
            el('div', {class: 'section-title'}, 'Your events'), eventsTable.element, detail.element));
    return {element, refresh};
}
