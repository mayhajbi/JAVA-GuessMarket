import {createActions} from './actions.js';
import {getJson} from './api.js';
import {createPriceChart} from './chart.js';
import {showError} from './dialog.js';
import {el} from './dom.js';
import {
    EVENT_STATUSES, EVENT_TYPES, NOT_AVAILABLE, commission, decimal, displayName, optionalDecimal
} from './format.js';
import {createTable} from './table.js';

const BOOK_STATISTICS = ['LAST', 'BID', 'ASK', 'MID', 'SPREAD'];

function section(title, ...content) {
    return el('div', {class: 'section'}, el('div', {class: 'section-title'}, title), ...content);
}

function table(columns, rows, placeholder) {
    const created = createTable({columns, placeholder});
    created.setRows(rows);
    return created.element;
}

function summary(info, state) {
    const pairs = [
        ['Status', displayName(EVENT_STATUSES, info.status)],
        ['Type', displayName(EVENT_TYPES, info.type)],
        ['Commission', commission(info.commissionPercent, info.commissionType)],
        ['Market maker', info.marketMakerName],
        ['Account balance', decimal(info.accountBalance)],
        ['Commission collected', decimal(state.totalCommissionCollected)],
        ['Winning option', state.winningOptionName ?? NOT_AVAILABLE]
    ];
    return el('div', {class: 'summary'},
        ...pairs.flatMap(([name, value]) => [el('span', {class: 'field-name'}, name), el('span', {}, value)]));
}

function optionBook(option) {
    const values = [option.lastPrice, option.bestBid, option.bestAsk, option.midPrice, option.spread];
    const orderColumns = [
        {title: 'User', text: (order) => order.userName},
        {title: 'Quantity', text: (order) => order.quantity, numeric: true, sort: (order) => order.quantity},
        {title: 'Price', text: (order) => decimal(order.price), numeric: true, sort: (order) => order.price}
    ];
    return el('div', {class: 'option-book'},
        el('div', {class: 'section-title'}, option.optionName),
        el('div', {class: 'statistics'}, ...BOOK_STATISTICS.map((name, index) => el('div', {},
            el('div', {class: 'field-name'}, name), el('div', {}, optionalDecimal(values[index]))))),
        el('div', {class: 'field-name'}, 'Bids: buy orders'),
        table(orderColumns, option.bids, 'No buy orders.'),
        el('div', {class: 'field-name'}, 'Asks: sell orders'),
        table(orderColumns, option.asks, 'No sell orders.'));
}

function participantColumns(optionNames) {
    return [
        {title: 'User', text: (participant) => participant.userName},
        ...optionNames.flatMap((name, index) => [
            {title: `${name} shares`, numeric: true, text: (participant) => participant.sharesPerOption[index],
                sort: (participant) => participant.sharesPerOption[index]},
            {title: `${name} value`, numeric: true,
                text: (participant) => optionalDecimal(participant.holdingValuePerOption[index]),
                sort: (participant) => participant.holdingValuePerOption[index] ?? 0}
        ])
    ];
}

function lmsrSections(state) {
    return [
        section('Options', table([
            {title: 'Option', text: (option) => option.name},
            {title: 'Value', text: (option) => decimal(option.value), numeric: true, sort: (option) => option.value},
            {title: 'Shares', text: (option) => option.shares, numeric: true, sort: (option) => option.shares}
        ], state.optionStates, 'No options.')),
        section('Trading history, latest first', table([
            {title: 'Buyer', text: (trade) => trade.buyerName},
            {title: 'Option', text: (trade) => trade.optionName},
            {title: 'Shares', text: (trade) => trade.shares, numeric: true, sort: (trade) => trade.shares},
            {title: 'Shares cost', text: (trade) => decimal(trade.sharesCost), numeric: true, sort: (trade) => trade.sharesCost},
            {title: 'Commission', text: (trade) => decimal(trade.commissionPaid), numeric: true, sort: (trade) => trade.commissionPaid},
            {title: 'Total paid', text: (trade) => decimal(trade.totalPaid), numeric: true, sort: (trade) => trade.totalPaid}
        ], state.tradeHistory, 'No trades yet.'))
    ];
}

function orderBookSections(state) {
    const maximum = decimal(state.baseValue - 0.01);
    return [
        el('div', {class: 'option-books'}, ...state.options.map(optionBook)),
        section('Participants', table(participantColumns(state.eventInfo.optionNames), state.participants,
            'No participants yet.')),
        section('Trades, latest first', table([
            {title: 'Buyer', text: (trade) => trade.buyerName},
            {title: 'From', text: (trade) => trade.minted ? `Minted with ${trade.counterpartyName}` : trade.counterpartyName},
            {title: 'Option', text: (trade) => trade.optionName},
            {title: 'Shares', text: (trade) => trade.quantity, numeric: true, sort: (trade) => trade.quantity},
            {title: 'Price', text: (trade) => decimal(trade.price), numeric: true, sort: (trade) => trade.price},
            {title: 'Commission', text: (trade) => decimal(trade.commissionPaid), numeric: true, sort: (trade) => trade.commissionPaid}
        ], state.tradeHistory, 'No trades yet.'), el('div', {class: 'field-name'},
            `A winning share pays ${decimal(state.baseValue)}. Order prices range from 0.01 to ${maximum}. `
            + `Minting is ${state.mintAllowed ? 'allowed' : 'not allowed'}. `
            + `The market maker's initial investment is ${decimal(state.initialInvestment)}.`))
    ];
}

/**
 * The details of the event that is selected: its summary, its options or order books, its trades and the
 * graph of its prices. Shows a message while no event is selected.
 */
export function createEventDetail(onChanged) {
    const placeholder = el('div', {class: 'placeholder'}, 'Select an event to see its details.');
    const head = el('div', {class: 'details-part'});
    const main = el('div', {class: 'details-part'});
    const tail = el('div', {class: 'details-part'});
    const actions = createActions(onChanged);
    const details = el('div', {class: 'details'}, head, main, actions.element, tail);
    details.hidden = true;
    let currentEvent = null;
    let user = null;
    let shown = null;
    let shownKey = null;

    // The parts of an event before the actions (as in the sketch); the rest comes after them.
    function render() {
        const {state, prices} = shown;
        const info = state.eventInfo;
        const key = JSON.stringify(shown);
        if (key !== shownKey) {
            shownKey = key;
            const isLmsr = info.type === 'LMSR';
            const parts = isLmsr ? lmsrSections(state) : orderBookSections(state);
            const beforeActions = isLmsr ? 1 : 2;
            const chart = createPriceChart(prices);
            head.replaceChildren(el('div', {class: 'section-title'}, info.name), el('div', {}, info.description),
                summary(info, state));
            main.replaceChildren(...parts.slice(0, beforeActions));
            tail.replaceChildren(...parts.slice(beforeActions), section('Price over time', chart
                ?? el('div', {class: 'placeholder'},
                    'No prices yet. The graph appears once the event is open and traded.')));
        }
        actions.element.hidden = user === null;
        if (user !== null) {
            actions.show(state, user);
        }
    }

    async function load(event, timeoutSeconds) {
        const stateUrl = event.type === 'LMSR' ? '/event/market' : '/event/orderbook';
        const [state, prices] = await Promise.all(
            [getJson(`${stateUrl}?id=${event.id}`, timeoutSeconds),
                getJson(`/event/prices?id=${event.id}`, timeoutSeconds)]);
        if (currentEvent !== null && currentEvent.id === event.id) {
            shown = {state, prices};
            render();
            placeholder.hidden = true;
            details.hidden = false;
        }
    }

    return {
        element: el('div', {class: 'event-detail'}, placeholder, details),

        /** @param event the selected event (as in the list of events), or null when none is selected */
        async show(event) {
            if (event === null) {
                currentEvent = null;
                shown = null;
                shownKey = null;
                placeholder.hidden = false;
                details.hidden = true;
                return;
            }
            if (currentEvent !== null && event.id === currentEvent.id) {
                return;
            }
            currentEvent = event;
            shown = null;
            try {
                await load(event);
            } catch (error) {
                showError(error);
            }
        },

        /** Pulls the details of the shown event again. */
        async refresh(timeoutSeconds) {
            if (currentEvent !== null) {
                await load(currentEvent, timeoutSeconds);
            }
        },

        /** @param newUser the details of the user who is logged in, who performs the actions */
        setUser(newUser) {
            user = newUser;
            if (shown !== null) {
                render();
            }
        }
    };
}
