@echo off
rem Starts the Guess Market web client at http://localhost:3000
rem The Guess Market server (Tomcat) must already run at localhost:8080.
cd /d "%~dp0"
where node >nul 2>nul
if errorlevel 1 (
    echo Node JS was not found. Install Node JS, open a new window and run this file again.
    pause
    exit /b 1
)
start "" http://localhost:3000
node server.js
pause
