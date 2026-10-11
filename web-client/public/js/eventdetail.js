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
export function createEventDetail() {
    const placeholder = el('div', {class: 'placeholder'}, 'Select an event to see its details.');
    const details = el('div', {class: 'details'});
    details.hidden = true;
    let currentId = null;

    function render(state, prices) {
        const info = state.eventInfo;
        const chart = createPriceChart(prices);
        details.replaceChildren(
            el('div', {class: 'section-title'}, info.name),
            el('div', {}, info.description),
            summary(info, state),
            ...(info.type === 'LMSR' ? lmsrSections(state) : orderBookSections(state)),
            section('Price over time', chart ?? el('div', {class: 'placeholder'},
                'No prices yet. The graph appears once the event is open and traded.')));
    }

    return {
        element: el('div', {class: 'event-detail'}, placeholder, details),

        /** @param event the selected event (as in the list of events), or null when none is selected */
        async show(event) {
            if (event === null) {
                currentId = null;
                placeholder.hidden = false;
                details.hidden = true;
                return;
            }
            if (event.id === currentId) {
                return;
            }
            currentId = event.id;
            const stateUrl = event.type === 'LMSR' ? '/event/market' : '/event/orderbook';
            try {
                const [state, prices] = await Promise.all(
                    [getJson(`${stateUrl}?id=${event.id}`), getJson(`/event/prices?id=${event.id}`)]);
                if (currentId === event.id) {
                    render(state, prices);
                    placeholder.hidden = true;
                    details.hidden = false;
                }
            } catch (error) {
                showError(error);
            }
        }
    };
}
