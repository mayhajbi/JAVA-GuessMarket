import {getJson} from './api.js';
import {createAccountScreen} from './account.js';
import {showError} from './dialog.js';
import {el} from './dom.js';
import {createEventsScreen} from './events.js';
import {showLogin} from './login.js';

const screen = document.getElementById('screen');

// The application after the login: a tab for the events and a tab for the account of the user. An action
// on any of them refreshes both, because it changes what both show.
function showApp(userName) {
    let events;
    let account;
    const refreshAll = () => Promise.all([events.refresh(), account.refresh()]);
    events = createEventsScreen(refreshAll);
    account = createAccountScreen(refreshAll);

    const screens = [{name: 'Events', content: events.element}, {name: 'Account', content: account.element}];
    const tabs = screens.map((one) => el('button', {type: 'button', class: 'tab', onclick: () => select(one)}, one.name));
    function select(chosen) {
        screens.forEach((one, index) => {
            one.content.hidden = one !== chosen;
            tabs[index].classList.toggle('selected', one === chosen);
        });
    }
    select(screens[0]);

    screen.replaceChildren(
        el('div', {class: 'header'}, el('span', {class: 'app-title'}, 'Guess Market'),
            el('span', {class: 'field-name'}, `Logged in as '${userName}'`)),
        el('div', {class: 'tabs'}, ...tabs),
        ...screens.map((one) => one.content));
}

// A session that is still valid (for example after a refresh of the page) goes straight to the application.
async function start() {
    try {
        showApp((await getJson('/account')).name);
    } catch (error) {
        showLogin(screen, showApp);
        if (error.status !== 401) {
            showError(error);
        }
    }
}

start();
