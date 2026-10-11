import {getJson, post, postJson} from './api.js';
import {confirm, showError, showInformation} from './dialog.js';
import {el} from './dom.js';
import {decimal, optionalDecimal} from './format.js';
import {readDecimal, readWholeNumber} from './inputs.js';

const BLOCKING_WARNING = '\n\nYour balance will drop below zero, and you will be blocked from trading until '
    + 'you load funds.';
const BLOCKED_TITLE_SUFFIX = ' - you are blocked';
const BLOCKED_REPORT = '\n\nYou are blocked because your balance is below zero. Load funds to trade again.';
const BUYING_HEADER = 'Buy shares';
const ORDER_HEADER = 'Place an order';
const CHOOSE_OPTION = 'Choose an option';

function optionSelect(optionNames, prompt) {
    return el('select', {}, el('option', {value: '', disabled: '', selected: ''}, prompt),
        ...optionNames.map((name, index) => el('option', {value: String(index)}, name)));
}

function selectedIndex(select) {
    return select.value === '' ? -1 : Number(select.value);
}

function row(...children) {
    return el('div', {class: 'action-row'}, ...children);
}

function reportResult(header, details, isBlocked) {
    return isBlocked
        ? showInformation(header + BLOCKED_TITLE_SUFFIX, details + BLOCKED_REPORT)
        : showInformation(header, details);
}

function describeQuote(quote, isBlocked) {
    return `Buy ${quote.shares} shares of '${quote.optionName}'?\n`
        + `Shares cost: ${decimal(quote.sharesCost)}\n`
        + `Commission: ${decimal(quote.commissionPaid)}\n`
        + `Total: ${decimal(quote.totalPaid)}\n`
        + `Balance after: ${decimal(quote.buyerBalance)}\n`
        + 'The price can change if other users trade before you confirm.'
        + (isBlocked ? BLOCKING_WARNING : '');
}

function describeOrderResult(userName, side, optionName, quantity, price, result) {
    let text = `You placed an order to ${side.toLowerCase()} ${quantity} shares of '${optionName}' at ${decimal(price)}.\n`;
    const tradeLines = [];
    for (const trade of result.trades) {
        const commission = trade.commissionPaid > 0 ? `, commission ${decimal(trade.commissionPaid)}` : '';
        if (side === 'BUY' && trade.buyerName === userName) {
            tradeLines.push(trade.minted
                ? `  ${trade.quantity} new shares minted at ${decimal(trade.price)}, matched with `
                    + `${trade.counterpartyName}'s order${commission}`
                : `  ${trade.quantity} shares bought from ${trade.counterpartyName} at ${decimal(trade.price)}${commission}`);
        } else if (side === 'SELL' && trade.counterpartyName === userName && !trade.minted) {
            tradeLines.push(`  ${trade.quantity} shares sold to ${trade.buyerName} at ${decimal(trade.price)}`);
        }
    }
    text += tradeLines.length === 0 ? 'No matching order yet.\n' : `Trades:\n${tradeLines.join('\n')}\n`;
    return text + `${result.filledQuantity} of ${quantity} shares matched; ${result.restingQuantity} are waiting `
        + `in the order book.\nYour balance: ${decimal(result.userBalance)}`;
}

function availableActionsHint(info, user, isMarketMaker) {
    if (info.status === 'CLOSED') {
        return 'This event is closed. No further actions are possible.';
    }
    if (user.blocked) {
        return 'You are blocked because your balance is below zero. Load funds to open or create events, buy '
            + 'shares or place orders.'
            + (isMarketMaker && info.status === 'ACTIVE' ? ' As the market maker, you can still close this event.' : '');
    }
    if (info.status === 'INACTIVE' && !isMarketMaker) {
        return `This event is not open yet. Only its market maker, '${info.marketMakerName}', can open it.`;
    }
    return '';
}

function positionRows(info, position) {
    const rows = info.optionNames.map((name, index) => [name,
        `${position.sharesPerOption[index]} shares, value ${optionalDecimal(position.holdingValuePerOption[index])}, `
        + `paid ${decimal(position.paidPerOption[index])}`]);
    if (position.initialInvestmentPaid > 0) {
        rows.push(['Initial investment paid', decimal(position.initialInvestmentPaid)]);
    }
    rows.push(['Commission paid', decimal(position.commissionPaid)],
        ['Received from sales and payout', decimal(position.received)],
        ['Profit / loss', info.status === 'CLOSED' ? decimal(position.profitOrLoss) : 'Shown when the event is closed']);
    return rows;
}

/**
 * The actions of the user who is logged in on the shown event: opening, buying, ordering and closing, and,
 * in an order book event, the position of the user. The controls are rebuilt only when what they depend on
 * changed, so that what the user typed in them stays while the data of the event is refreshed.
 *
 * @param onChanged called after an action changed the data in the server
 */
export function createActions(onChanged) {
    const title = el('div', {class: 'section-title'}, 'Actions');
    const blockedLabel = el('span', {class: 'blocked'}, 'Blocked');
    const controls = el('div', {class: 'action-controls'});
    const hint = el('div', {class: 'placeholder'});
    const position = el('div', {class: 'position'});
    const element = el('div', {class: 'actions'},
        el('div', {class: 'action-row'}, title, blockedLabel), controls, hint, position);
    let state = null;
    let user = null;
    let controlsKey = null;

    const guarded = (action) => async () => {
        try {
            await action();
        } catch (error) {
            showError(error);
        }
    };

    function readQuantity(input, header) {
        const text = input.value.trim();
        const quantity = readWholeNumber(text);
        if (quantity === null) {
            showInformation(header, `'${text}' is not a valid quantity. Enter a whole number of shares, for example 10.`);
            input.focus();
        }
        return quantity;
    }

    async function open() {
        const info = state.eventInfo;
        const opened = await postJson(`/event/open?id=${info.id}`);
        const paid = decimal(opened.accountBalance);
        let message = `'${info.name}' is now open. You paid the initial subsidy of ${paid}, and trading has started.`;
        if (info.type === 'ORDER_BOOK') {
            const book = await getJson(`/event/orderbook?id=${info.id}`);
            const pairs = book.participants.find((participant) => participant.userName === user.name).sharesPerOption[0];
            message = `'${info.name}' is now open. You paid the initial investment of ${paid} and received ${pairs} `
                + 'shares of each option, which you can now offer for sale. Trading has started.';
        }
        onChanged();
        await showInformation('Event opened', message);
    }

    async function buy(optionSelectElement, quantityInput) {
        const info = state.eventInfo;
        const optionIndex = selectedIndex(optionSelectElement);
        if (optionIndex < 0) {
            await showInformation(BUYING_HEADER, 'Select an option to buy.');
            return;
        }
        const quantity = readQuantity(quantityInput, BUYING_HEADER);
        if (quantity === null) {
            return;
        }
        const quote = await getJson(`/event/quote?id=${info.id}&option=${optionIndex}&quantity=${quantity}`);
        if (!await confirm('Confirm purchase', describeQuote(quote, quote.buyerBlocked))) {
            return;
        }
        const result = await postJson('/event/buy', {eventId: info.id, optionIndex, quantity});
        quantityInput.value = '';
        onChanged();
        await reportResult('Purchase completed', `You bought ${result.shares} shares of '${result.optionName}' for `
            + `${decimal(result.totalPaid)}.\nYour balance: ${decimal(result.buyerBalance)}`, result.buyerBlocked);
    }

    async function placeOrder(side, optionSelectElement, quantityInput, priceInput) {
        const info = state.eventInfo;
        const optionIndex = selectedIndex(optionSelectElement);
        if (optionIndex < 0) {
            await showInformation(ORDER_HEADER, 'Select an option to trade.');
            return;
        }
        const quantity = readQuantity(quantityInput, ORDER_HEADER);
        if (quantity === null) {
            return;
        }
        const priceText = priceInput.value.trim();
        const price = readDecimal(priceText);
        if (price === null) {
            await showInformation(ORDER_HEADER,
                `'${priceText}' is not a valid price. Enter the price per share, for example 0.45.`);
            priceInput.focus();
            return;
        }
        const orderSide = side();
        const result = await postJson('/event/order',
            {eventId: info.id, side: orderSide, optionIndex, quantity, price});
        quantityInput.value = '';
        priceInput.value = '';
        onChanged();
        await reportResult('Order placed', describeOrderResult(user.name, orderSide, info.optionNames[optionIndex],
            quantity, price, result), result.userBlocked);
    }

    async function close(winnerSelect) {
        const info = state.eventInfo;
        const winnerIndex = selectedIndex(winnerSelect);
        if (winnerIndex < 0) {
            await showInformation('Close the event', 'Select the winning option to close the event.');
            return;
        }
        const winnerName = info.optionNames[winnerIndex];
        if (!await confirm(`Close '${info.name}'`, `Close '${info.name}' with '${winnerName}' as the winning option? `
            + 'Winners are paid, and the event can no longer be traded or reopened.')) {
            return;
        }
        await post(`/event/close?id=${info.id}&winner=${winnerIndex}`);
        onChanged();
        await showInformation('Event closed', `'${info.name}' is closed. '${winnerName}' won, and the winners were `
            + 'paid. You received the commission and the remaining event balance.');
    }

    function buildControls(info, isMarketMaker) {
        const names = info.optionNames;
        const items = [];
        if (info.status === 'INACTIVE') {
            items.push(el('div', {}, el('button', {type: 'button', disabled: isMarketMaker && !user.blocked ? null : '',
                    onclick: guarded(open)}, 'Open event'),
                el('div', {class: 'field-name'}, info.type === 'LMSR'
                    ? 'The market maker pays the initial subsidy and trading starts.'
                    : 'The market maker pays the initial investment, receives the initial shares and trading starts.')));
        }
        if (info.status === 'ACTIVE' && info.type === 'LMSR') {
            const quantity = el('input', {placeholder: 'Quantity', size: '8'});
            const option = optionSelect(names, CHOOSE_OPTION);
            items.push(row(el('span', {class: 'field-name'}, 'Buy'), quantity,
                el('span', {class: 'field-name'}, 'shares of'), option,
                el('button', {type: 'button', disabled: user.blocked ? '' : null,
                    onclick: guarded(() => buy(option, quantity))}, 'Buy')));
        }
        if (info.status === 'ACTIVE' && info.type === 'ORDER_BOOK') {
            const side = el('select', {}, el('option', {value: 'BUY'}, 'Buy'), el('option', {value: 'SELL'}, 'Sell'));
            const quantity = el('input', {placeholder: 'Quantity', size: '8'});
            const option = optionSelect(names, CHOOSE_OPTION);
            const price = el('input', {placeholder: 'Price', size: '7'});
            const submit = guarded(() => placeOrder(() => side.value, option, quantity, price));
            price.addEventListener('keydown', (event) => event.key === 'Enter' && submit());
            items.push(row(side, quantity, el('span', {class: 'field-name'}, 'shares of'), option,
                el('span', {class: 'field-name'}, 'at'), price,
                el('button', {type: 'button', disabled: user.blocked ? '' : null, onclick: submit}, 'Place order')));
        }
        if (info.status === 'ACTIVE' && isMarketMaker) {
            const winner = optionSelect(names, 'Choose the winner');
            items.push(row(el('span', {class: 'field-name'}, 'Winning option'), winner,
                el('button', {type: 'button', onclick: guarded(() => close(winner))}, 'Close event')));
        }
        controls.replaceChildren(...items);
    }

    function showPosition(info) {
        const participant = info.type === 'ORDER_BOOK'
            ? state.participants.find((one) => one.userName === user.name)
            : undefined;
        position.hidden = participant === undefined;
        if (participant === undefined) {
            return;
        }
        position.replaceChildren(el('div', {class: 'section-title'}, 'Your position'),
            el('div', {class: 'position-grid'}, ...positionRows(info, participant).flatMap(([name, value]) =>
                [el('span', {class: 'field-name'}, name), el('span', {}, value)])));
    }

    return {
        element,

        /**
         * @param newState the state of the shown event (the details that are sent for its type)
         * @param newUser  the details of the user who is logged in
         */
        show(newState, newUser) {
            state = newState;
            user = newUser;
            const info = state.eventInfo;
            const isMarketMaker = user.name === info.marketMakerName;
            const key = JSON.stringify([info.id, info.status, info.type, info.marketMakerName, info.optionNames,
                user.name, user.blocked]);
            if (key !== controlsKey) {
                controlsKey = key;
                buildControls(info, isMarketMaker);
                blockedLabel.hidden = !user.blocked;
                const text = availableActionsHint(info, user, isMarketMaker);
                hint.textContent = text;
                hint.hidden = text === '';
            }
            showPosition(info);
        }
    };
}
