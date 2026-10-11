import {getJson, isUnreachable} from './api.js';
import {createAccountScreen} from './account.js';
import {MAX_FAILURES, REFRESH_RATE, REFRESH_TIMEOUT_SECONDS} from './constants.js';
import {showError} from './dialog.js';
import {el} from './dom.js';
import {createEventsScreen} from './events.js';
import {showLogin} from './login.js';

const screen = document.getElementById('screen');
const UNAUTHORIZED = 401;

// The application after the login: a tab for the events and a tab for the account of the user. An action
// on any of them refreshes both, because it changes what both show. The screen that is shown pulls its
// data every REFRESH_RATE.
function showApp(userName) {
    let events;
    let account;
    const refreshAll = async () => {
        try {
            await Promise.all([events.refresh(), account.refresh()]);
        } catch (error) {
            showError(error);
        }
    };
    events = createEventsScreen(refreshAll);
    account = createAccountScreen(refreshAll);

    const screens = [{name: 'Events', screen: events}, {name: 'Account', screen: account}];
    let shown = screens[0];
    const tabs = screens.map((one) => el('button', {type: 'button', class: 'tab', onclick: () => select(one)}, one.name));
    function select(chosen) {
        shown = chosen;
        screens.forEach((one, index) => {
            one.screen.element.hidden = one !== chosen;
            tabs[index].classList.toggle('selected', one === chosen);
        });
        pull();
    }

    const serverStatus = el('div', {class: 'server-status'},
        'Server not reachable. The screens are locked until the server answers again.');
    serverStatus.hidden = true;
    const content = el('div', {class: 'app'}, el('div', {class: 'tabs'}, ...tabs),
        ...screens.map((one) => one.screen.element));
    screen.replaceChildren(
        el('div', {class: 'header'}, el('span', {class: 'app-title'}, 'Guess Market'),
            el('span', {class: 'field-name'}, `Logged in as '${userName}'`)),
        serverStatus, content);

    // One pull at a time: a cycle that finds the previous one still waiting is skipped. After MAX_FAILURES
    // cycles in a row without an answer the server is reported and the screens are locked until it answers.
    let waiting = false;
    let failures = 0;
    function setReachable(reachable) {
        serverStatus.hidden = reachable;
        content.inert = !reachable;
    }
    async function pull() {
        if (waiting) {
            return;
        }
        waiting = true;
        try {
            await shown.screen.refresh(REFRESH_TIMEOUT_SECONDS);
            failures = 0;
            setReachable(true);
        } catch (error) {
            if (error.status === UNAUTHORIZED) {
                clearInterval(timer);
                showLogin(screen, showApp);
            } else if (isUnreachable(error)) {
                failures++;
                if (failures >= MAX_FAILURES) {
                    setReachable(false);
                }
            }
        } finally {
            waiting = false;
        }
    }
    const timer = setInterval(pull, REFRESH_RATE);
    select(screens[0]);
}

// A session that is still valid (for example after a refresh of the page) goes straight to the application.
async function start() {
    try {
        showApp((await getJson('/account')).name);
    } catch (error) {
        showLogin(screen, showApp);
        if (error.status !== UNAUTHORIZED) {
            showError(error);
        }
    }
}

start();
