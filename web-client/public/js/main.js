import {getJson} from './api.js';
import {showError} from './dialog.js';
import {el} from './dom.js';
import {createEventsScreen} from './events.js';
import {showLogin} from './login.js';

const screen = document.getElementById('screen');

function showEvents(userName) {
    screen.replaceChildren(
        el('div', {class: 'header'}, el('span', {class: 'app-title'}, 'Guess Market'),
            el('span', {class: 'field-name'}, `Logged in as '${userName}'`)),
        createEventsScreen());
}

// A session that is still valid (for example after a refresh of the page) goes straight to the events.
async function start() {
    try {
        showEvents((await getJson('/account')).name);
    } catch (error) {
        showLogin(screen, showEvents);
        if (error.status !== 401) {
            showError(error);
        }
    }
}

start();
