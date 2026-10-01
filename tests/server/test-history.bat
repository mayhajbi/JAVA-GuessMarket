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
call :expect "no-session" GET 401 "account/history" nocookie "not logged in" ""

curl.exe -s -o nul -c "%COOKIES%" "%BASE_URL%/login?username=%GM_USER%"

rem --- a new user has a list (a list of points, so the answer starts with a bracket) ---
call :expect "history-of-a-new-user" GET 200 "account/history" cookie "[" ""

rem --- every deposit adds a point with the new balance ---
call :expect "deposit-first" POST 200 "account/deposit?amount=100.5" cookie ":100.5," ""
call :expect "history-after-first-deposit" GET 200 "account/history" cookie ":100.5}" ""
call :expect "deposit-second" POST 200 "account/deposit?amount=10" cookie ":110.5," ""
call :expect "history-after-second-deposit" GET 200 "account/history" cookie ":110.5}" ""
call :expect "history-keeps-the-first-point" GET 200 "account/history" cookie ":100.5}" ""
call :expect "history-ignores-name" GET 200 "account/history?username=someone_else" cookie ":110.5}" "someone_else"

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
