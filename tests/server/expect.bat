@echo off
rem ---------------------------------------------------------------------------
rem  One request and what its answer has to be, shared by the server checks.
rem  Usage, from a check script that set BASE_URL, BODY_FILE, COOKIES, NO_COOKIES
rem  and FAILS, and turned on delayed expansion:
rem    call "%~dp0expect.bat" <name> <method> <status> <path> <cookie or nocookie>
rem         <text the body must have, or ""> <text it must not have, or "">
rem  Prints one [PASS]/[FAIL] line and adds a failure to FAILS.
rem ---------------------------------------------------------------------------
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
