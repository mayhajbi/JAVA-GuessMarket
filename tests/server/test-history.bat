@echo off
rem ---------------------------------------------------------------------------
rem  Checks the /account/history endpoint (the balance of the user over time):
rem  refused without a session (401); a logged in user gets a list; every
rem  deposit adds a point with the new balance; the user comes from the
rem  session, a name in the query is ignored. Requires the guessmarket WAR to
rem  already be running locally on Tomcat (see ..\README.md) before this runs.
rem  Prints one [PASS]/[FAIL] line per check; exits with the number of
rem  failures (0 = all passed).
rem ---------------------------------------------------------------------------
setlocal enabledelayedexpansion

set BASE_URL=http://localhost:8080/guessmarket
set GM_USER=history_%RANDOM%
set FAILS=0
set BODY_FILE=%TEMP%\gm_history_body.txt
set COOKIES=%TEMP%\gm_history_cookies.txt
set NO_COOKIES=%TEMP%\gm_history_no_such_cookies.txt
if exist "%COOKIES%" del /q "%COOKIES%"

rem --- no session ---
call "%~dp0expect.bat" "no-session" GET 401 "account/history" nocookie "not logged in" ""

curl.exe -s -o nul -c "%COOKIES%" -X POST "%BASE_URL%/login?username=%GM_USER%"

rem --- a new user has a list (a list of points, so the answer starts with a bracket) ---
call "%~dp0expect.bat" "history-of-a-new-user" GET 200 "account/history" cookie "[" ""

rem --- every deposit adds a point with the new balance ---
call "%~dp0expect.bat" "deposit-first" POST 200 "account/deposit?amount=100.5" cookie ":100.5," ""
call "%~dp0expect.bat" "history-after-first-deposit" GET 200 "account/history" cookie ":100.5}" ""
call "%~dp0expect.bat" "deposit-second" POST 200 "account/deposit?amount=10" cookie ":110.5," ""
call "%~dp0expect.bat" "history-after-second-deposit" GET 200 "account/history" cookie ":110.5}" ""
call "%~dp0expect.bat" "history-keeps-the-first-point" GET 200 "account/history" cookie ":100.5}" ""
call "%~dp0expect.bat" "history-ignores-name" GET 200 "account/history?username=someone_else" cookie ":110.5}" "someone_else"

del /q "%BODY_FILE%" "%COOKIES%" >nul 2>nul
set RESULT=%FAILS%
endlocal & exit /b %RESULT%
