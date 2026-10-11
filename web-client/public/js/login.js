import {post} from './api.js';
import {showError} from './dialog.js';

/**
 * The login screen: the user types a name and the server logs the user in. When the server refuses the
 * name, its message is shown in a dialog and what was typed stays, so that it is easy to correct.
 *
 * @param onLoggedIn called with the name once the server logged the user in
 */
export function showLogin(screen, onLoggedIn) {
    screen.innerHTML = `
        <form class="login">
            <div class="app-title">Guess Market</div>
            <label for="user-name">Enter your user name to log in.</label>
            <input id="user-name" placeholder="User name" autocomplete="off" autofocus>
            <button type="submit">Log in</button>
        </form>`;
    const input = screen.querySelector('#user-name');
    screen.querySelector('form').addEventListener('submit', async (event) => {
        event.preventDefault();
        try {
            await post('/login?username=' + encodeURIComponent(input.value));
            onLoggedIn(input.value.trim());
        } catch (error) {
            showError(error);
        }
    });
}
