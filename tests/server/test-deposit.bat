@echo off
rem ---------------------------------------------------------------------------
rem  Checks the /account/deposit endpoint: refused without a session (401) and
rem  as GET (400); a wrong amount (missing, not a number, zero, negative) is 400
rem  with the reason; a valid deposit answers with the new balance and shows up
rem  in /account/log; the user comes from the session, a name in the query is
rem  ignored. Requires the guessmarket WAR to already be running locally on
rem  Tomcat (see ..\README.md) before this runs.
rem  Prints one [PASS]/[FAIL] line per check; exits with the number of
rem  failures (0 = all passed).
rem ---------------------------------------------------------------------------
setlocal enabledelayedexpansion

set BASE_URL=http://localhost:8080/guessmarket
set GM_USER=deposit_%RANDOM%
set FAILS=0
set BODY_FILE=%TEMP%\gm_deposit_body.txt
set COOKIES=%TEMP%\gm_deposit_cookies.txt
set NO_COOKIES=%TEMP%\gm_deposit_no_such_cookies.txt
if exist "%COOKIES%" del /q "%COOKIES%"

rem --- no session ---
call "%~dp0expect.bat" "no-session" POST 401 "account/deposit?amount=10" nocookie "not logged in" ""

curl.exe -s -o nul -c "%COOKIES%" -X POST "%BASE_URL%/login?username=%GM_USER%"

rem --- a wrong request ---
call "%~dp0expect.bat" "deposit-as-get" GET 400 "account/deposit?amount=10" cookie "must be sent as POST" ""
call "%~dp0expect.bat" "deposit-no-amount" POST 400 "account/deposit" cookie "is missing" ""
call "%~dp0expect.bat" "deposit-not-a-number" POST 400 "account/deposit?amount=abc" cookie "must be a number" ""
call "%~dp0expect.bat" "deposit-zero" POST 400 "account/deposit?amount=0" cookie "greater than 0" ""
call "%~dp0expect.bat" "deposit-negative" POST 400 "account/deposit?amount=-5" cookie "greater than 0" ""
call "%~dp0expect.bat" "deposit-infinity" POST 400 "account/deposit?amount=Infinity" cookie "greater than 0" ""

rem --- a valid deposit: a new user starts with 0, the answer has the new balance, and the log has the movement ---
call "%~dp0expect.bat" "deposit-valid" POST 200 "account/deposit?amount=100.5" cookie ":100.5," ""
call "%~dp0expect.bat" "deposit-again" POST 200 "account/deposit?amount=10" cookie ":110.5," ""
call "%~dp0expect.bat" "deposit-in-log" GET 200 "account/log" cookie "100.5" ""
call "%~dp0expect.bat" "deposit-ignores-name" POST 200 "account/deposit?amount=1&username=someone_else" cookie "%GM_USER%" "someone_else"

del /q "%BODY_FILE%" "%COOKIES%" >nul 2>nul
set RESULT=%FAILS%
endlocal & exit /b %RESULT%
