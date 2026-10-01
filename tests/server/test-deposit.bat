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
call :expect "no-session" POST 401 "account/deposit?amount=10" nocookie "not logged in" ""

curl.exe -s -o nul -c "%COOKIES%" "%BASE_URL%/login?username=%GM_USER%"

rem --- a wrong request ---
call :expect "deposit-as-get" GET 400 "account/deposit?amount=10" cookie "must be sent as POST" ""
call :expect "deposit-no-amount" POST 400 "account/deposit" cookie "is missing" ""
call :expect "deposit-not-a-number" POST 400 "account/deposit?amount=abc" cookie "must be a number" ""
call :expect "deposit-zero" POST 400 "account/deposit?amount=0" cookie "positive number" ""
call :expect "deposit-negative" POST 400 "account/deposit?amount=-5" cookie "positive number" ""
call :expect "deposit-infinity" POST 400 "account/deposit?amount=Infinity" cookie "positive number" ""

rem --- a valid deposit: a new user starts with 0, the answer has the new balance, and the log has the movement ---
call :expect "deposit-valid" POST 200 "account/deposit?amount=100.5" cookie ":100.5," ""
call :expect "deposit-again" POST 200 "account/deposit?amount=10" cookie ":110.5," ""
call :expect "deposit-in-log" GET 200 "account/log" cookie "100.5" ""
call :expect "deposit-ignores-name" POST 200 "account/deposit?amount=1&username=someone_else" cookie "%GM_USER%" "someone_else"

del /q "%BODY_FILE%" "%COOKIES%" >nul 2>nul
set RESULT=%FAILS%
endlocal & exit /b %RESULT%

rem  :expect <name> <method> <status> <path> <cookie or nocookie> <text the body must have> <text it must not have>
:expect
set COOKIE_FILE=%NO_COOKIES%
if "%~5"=="cookie" set COOKIE_FILE=%COOKIES%
for /f %%s in ('curl.exe -s -o "%BODY_FILE%" -w "%%{http_code}" -X %~2 -b "%COOKIE_FILE%" "%BASE_URL%/%~4"') do set STATUS=%%s
if not "!STATUS!"=="%~3" (
    echo [FAIL] %~1: expected status %~3, got !STATUS!
    type "%BODY_FILE%"
    echo.
    set /a FAILS+=1
    exit /b 0
)
if not "%~6"=="" (
    findstr /i /c:"%~6" "%BODY_FILE%" >nul
    if errorlevel 1 (
        echo [FAIL] %~1: the answer should have [%~6], and it is:
        type "%BODY_FILE%"
        echo.
        set /a FAILS+=1
        exit /b 0
    )
)
if not "%~7"=="" (
    findstr /i /c:"%~7" "%BODY_FILE%" >nul
    if not errorlevel 1 (
        echo [FAIL] %~1: the answer should not have [%~7], and it is:
        type "%BODY_FILE%"
        echo.
        set /a FAILS+=1
        exit /b 0
    )
)
echo [PASS] %~1: %~3
exit /b 0
