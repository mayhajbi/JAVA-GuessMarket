import {getJson} from './api.js';
import {showError} from './dialog.js';
import {showLogin} from './login.js';

const screen = document.getElementById('screen');

function showEvents(userName) {
    screen.innerHTML = '<div class="app-title">Events</div><p></p>';
    screen.querySelector('p').textContent = `Logged in as '${userName}'.`;
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
