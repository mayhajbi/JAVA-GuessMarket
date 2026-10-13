// Serves the web client and forwards every /guessmarket request to the Guess Market server (Tomcat),
// so that the browser talks to one origin only and the server needs no change.
const http = require('http');
const fs = require('fs');
const path = require('path');

const PORT = 3000;
const CONTEXT_PATH = '/guessmarket';
const SERVER_HOST = 'localhost';
const SERVER_PORT = 8080;
const PUBLIC_DIR = path.join(__dirname, 'public');
const CONTENT_TYPES = {
    '.html': 'text/html; charset=utf-8',
    '.css': 'text/css; charset=utf-8',
    '.js': 'text/javascript; charset=utf-8',
    '.svg': 'image/svg+xml',
    '.ico': 'image/x-icon'
};

function sendError(response, status, title, message) {
    response.writeHead(status, {'Content-Type': 'application/json; charset=utf-8'});
    response.end(JSON.stringify({title, message}));
}

function forward(request, response) {
    const headers = {...request.headers, host: `${SERVER_HOST}:${SERVER_PORT}`};
    const upstream = http.request(
        {host: SERVER_HOST, port: SERVER_PORT, path: request.url, method: request.method, headers},
        (answer) => {
            response.writeHead(answer.statusCode, answer.headers);
            answer.pipe(response);
        });
    upstream.on('error', () => sendError(response, 502, 'Server not reachable',
        'The Guess Market server does not answer. Check that Tomcat is running with the guessmarket application, and try again.'));
    request.pipe(upstream);
}

function serveFile(request, response) {
    const urlPath = decodeURIComponent(new URL(request.url, 'http://localhost').pathname);
    const file = path.join(PUBLIC_DIR, urlPath === '/' ? 'index.html' : urlPath);
    if (!file.startsWith(PUBLIC_DIR)) {
        return sendError(response, 403, 'Forbidden', 'This path is not available.');
    }
    fs.readFile(file, (error, content) => {
        if (error) {
            return sendError(response, 404, 'Not found', 'This page does not exist.');
        }
        response.writeHead(200, {'Content-Type': CONTENT_TYPES[path.extname(file)] || 'application/octet-stream'});
        response.end(content);
    });
}

const server = http.createServer((request, response) => {
    if (request.url === CONTEXT_PATH || request.url.startsWith(CONTEXT_PATH + '/')) {
        forward(request, response);
    } else {
        serveFile(request, response);
    }
});

server.on('error', (error) => {
    console.error(error.code === 'EADDRINUSE'
        ? `Port ${PORT} is already in use. Close the program that uses it (maybe another web client), and run this file again.`
        : error.message);
    process.exit(1);
});

server.listen(PORT, () => console.log(`Guess Market web client: http://localhost:${PORT}`));
