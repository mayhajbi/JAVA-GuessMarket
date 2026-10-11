// The one message dialog of the application.
const dialog = document.getElementById('message');
document.getElementById('message-ok').addEventListener('click', () => dialog.close());

export function showError(error) {
    document.getElementById('message-title').textContent = error.title ?? 'Unexpected error';
    document.getElementById('message-text').textContent = error.message;
    dialog.showModal();
}
