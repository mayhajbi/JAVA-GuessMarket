@echo off
rem ---------------------------------------------------------------------------
rem  Checks the read endpoints: /events, /event/market, /event/orderbook,
rem  /event/prices, /users, /account and /account/log - refused without a
rem  session (401), a faulty parameter gives 400 with a message, and a logged in
rem  user gets JSON about themselves only. The checks do not depend on what is
rem  already in the server. Requires the guessmarket WAR to already be running
rem  locally on Tomcat (see ..\README.md) before this runs.
rem  Prints one [PASS]/[FAIL] line per check; exits with the number of
rem  failures (0 = all passed).
rem ---------------------------------------------------------------------------
setlocal enabledelayedexpansion

set BASE_URL=http://localhost:8080/guessmarket
set GM_USER=read_api_%RANDOM%
set FAILS=0
set BODY_FILE=%TEMP%\gm_read_api_body.txt
set COOKIES=%TEMP%\gm_read_api_cookies.txt
set NO_COOKIES=%TEMP%\gm_read_api_no_such_cookies.txt
if exist "%COOKIES%" del /q "%COOKIES%"

rem --- every read is refused without a session ---
call "%~dp0expect.bat" "no-session-events" GET 401 "events" nocookie "" ""
call "%~dp0expect.bat" "no-session-market" GET 401 "event/market?id=1" nocookie "" ""
call "%~dp0expect.bat" "no-session-orderbook" GET 401 "event/orderbook?id=1" nocookie "" ""
call "%~dp0expect.bat" "no-session-prices" GET 401 "event/prices?id=1" nocookie "" ""
call "%~dp0expect.bat" "no-session-users" GET 401 "users" nocookie "" ""
call "%~dp0expect.bat" "no-session-account" GET 401 "account" nocookie "" ""
call "%~dp0expect.bat" "no-session-account-log" GET 401 "account/log" nocookie "" ""

rem --- log in; from here on the requests carry the session ---
for /f %%s in ('curl.exe -s -o nul -w "%%{http_code}" -c "%COOKIES%" "%BASE_URL%/login?username=%GM_USER%"') do set STATUS=%%s
if not "!STATUS!"=="200" (
    echo [FAIL] login-for-read-api: expected status 200, got !STATUS!
    set /a FAILS+=1
)

rem --- events: a list, the filter in the query, and a faulty filter ---
call "%~dp0expect.bat" "events-all" GET 200 "events" cookie "[" ""
call "%~dp0expect.bat" "events-filtered" GET 200 "events?types=LMSR&statuses=ACTIVE,inactive&commissions=ON_CLOSE" cookie "[" ""
call "%~dp0expect.bat" "events-empty-selection" GET 200 "events?types=" cookie "[]" ""
call "%~dp0expect.bat" "events-bad-filter" GET 400 "events?types=BOGUS" cookie "not one of" ""

rem --- the state of one event: the id is needed and has to exist ---
call "%~dp0expect.bat" "market-no-id" GET 400 "event/market" cookie "is missing" ""
call "%~dp0expect.bat" "market-bad-id" GET 400 "event/market?id=abc" cookie "whole number" ""
call "%~dp0expect.bat" "market-unknown-id" GET 400 "event/market?id=999999" cookie "" ""
call "%~dp0expect.bat" "orderbook-unknown-id" GET 400 "event/orderbook?id=999999" cookie "" ""
call "%~dp0expect.bat" "prices-unknown-id" GET 400 "event/prices?id=999999" cookie "" ""

rem --- the other users do not include me ---
call "%~dp0expect.bat" "users-without-me" GET 200 "users" cookie "[" "%GM_USER%"

rem --- my account is mine, whatever name is sent ---
call "%~dp0expect.bat" "account-mine" GET 200 "account" cookie "%GM_USER%" ""
call "%~dp0expect.bat" "account-ignores-name" GET 200 "account?username=someone_else" cookie "%GM_USER%" "someone_else"
call "%~dp0expect.bat" "account-log-empty" GET 200 "account/log" cookie "[]" ""

del /q "%BODY_FILE%" "%COOKIES%" >nul 2>nul
set RESULT=%FAILS%
endlocal & exit /b %RESULT%
