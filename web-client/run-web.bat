@echo off
rem Starts the Guess Market web client and opens it at http://localhost:3000
rem The Guess Market server (Tomcat) has to run at localhost:8080 with the guessmarket application.
cd /d "%~dp0"
where node >nul 2>nul
if errorlevel 1 (
    echo Node JS was not found. Install Node JS, open a new window and run this file again.
    pause
    exit /b 1
)
rem The browser is opened after the server had a moment to start.
start "" /min cmd /c "ping -n 3 127.0.0.1 >nul & start http://localhost:3000"
node server.js
pause
