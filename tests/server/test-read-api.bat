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
call :expect "no-session-events" 401 "events" "" "" ""
call :expect "no-session-market" 401 "event/market?id=1" "" "" ""
call :expect "no-session-orderbook" 401 "event/orderbook?id=1" "" "" ""
call :expect "no-session-prices" 401 "event/prices?id=1" "" "" ""
call :expect "no-session-users" 401 "users" "" "" ""
call :expect "no-session-account" 401 "account" "" "" ""
call :expect "no-session-account-log" 401 "account/log" "" "" ""

rem --- log in; from here on the requests carry the session ---
for /f %%s in ('curl.exe -s -o nul -w "%%{http_code}" -c "%COOKIES%" "%BASE_URL%/login?username=%GM_USER%"') do set STATUS=%%s
if not "!STATUS!"=="200" (
    echo [FAIL] login-for-read-api: expected status 200, got !STATUS!
    set /a FAILS+=1
)

rem --- events: a list, the filter in the query, and a faulty filter ---
call :expect "events-all" 200 "events" cookie "[" ""
call :expect "events-filtered" 200 "events?types=LMSR&statuses=ACTIVE,inactive&commissions=ON_CLOSE" cookie "[" ""
call :expect "events-empty-selection" 200 "events?types=" cookie "[]" ""
call :expect "events-bad-filter" 400 "events?types=BOGUS" cookie "not one of" ""

rem --- the state of one event: the id is needed and has to exist ---
call :expect "market-no-id" 400 "event/market" cookie "is missing" ""
call :expect "market-bad-id" 400 "event/market?id=abc" cookie "whole number" ""
call :expect "market-unknown-id" 400 "event/market?id=999999" cookie "" ""
call :expect "orderbook-unknown-id" 400 "event/orderbook?id=999999" cookie "" ""
call :expect "prices-unknown-id" 400 "event/prices?id=999999" cookie "" ""

rem --- the other users do not include me ---
call :expect "users-without-me" 200 "users" cookie "[" "%GM_USER%"

rem --- my account is mine, whatever name is sent ---
call :expect "account-mine" 200 "account" cookie "%GM_USER%" ""
call :expect "account-ignores-name" 200 "account?username=someone_else" cookie "%GM_USER%" "someone_else"
call :expect "account-log-empty" 200 "account/log" cookie "[]" ""

del /q "%BODY_FILE%" "%COOKIES%" >nul 2>nul
set RESULT=%FAILS%
endlocal & exit /b %RESULT%

rem ---------------------------------------------------------------------------
rem  :expect <name> <status> <path> <cookie or empty> <text the body must have> <text it must not have>
rem ---------------------------------------------------------------------------
:expect
set COOKIE_FILE=%NO_COOKIES%
if "%~4"=="cookie" set COOKIE_FILE=%COOKIES%
for /f %%s in ('curl.exe -s -o "%BODY_FILE%" -w "%%{http_code}" -b "%COOKIE_FILE%" "%BASE_URL%/%~3"') do set STATUS=%%s
if not "!STATUS!"=="%~2" (
    echo [FAIL] %~1: expected status %~2, got !STATUS!
    type "%BODY_FILE%"
    echo.
    set /a FAILS+=1
    exit /b 0
)
if not "%~5"=="" (
    findstr /l /c:"%~5" "%BODY_FILE%" >nul
    if errorlevel 1 (
        echo [FAIL] %~1: the answer should have [%~5], and it is:
        type "%BODY_FILE%"
        echo.
        set /a FAILS+=1
        exit /b 0
    )
)
if not "%~6"=="" (
    findstr /l /c:"%~6" "%BODY_FILE%" >nul
    if not errorlevel 1 (
        echo [FAIL] %~1: the answer should not have [%~6], and it is:
        type "%BODY_FILE%"
        echo.
        set /a FAILS+=1
        exit /b 0
    )
)
echo [PASS] %~1: %~2
exit /b 0
