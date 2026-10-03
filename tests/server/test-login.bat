@echo off
rem ---------------------------------------------------------------------------
rem  Checks the /login endpoint: a valid login, the same name from another
rem  session, and a missing name. Requires the guessmarket WAR to already be
rem  running locally on Tomcat (see ..\README.md) before this runs.
rem  Prints one [PASS]/[FAIL] line per check; exits with the number of
rem  failures (0 = all passed).
rem ---------------------------------------------------------------------------
setlocal enabledelayedexpansion

set BASE_URL=http://localhost:8080/guessmarket
set USERNAME=login_check_%RANDOM%
set FAILS=0
set TMP_BODY=%TEMP%\gm_login_check_body.txt
set TMP_COOKIES=%TEMP%\gm_login_check_cookies.txt
if exist "%TMP_COOKIES%" del /q "%TMP_COOKIES%"

rem --- a valid login returns 200 and a session cookie ---
for /f %%s in ('curl.exe -s -o "%TMP_BODY%" -w "%%{http_code}" -c "%TMP_COOKIES%" -X POST "%BASE_URL%/login?username=%USERNAME%"') do set STATUS=%%s
if "!STATUS!"=="200" (
    findstr /c:"JSESSIONID" "%TMP_COOKIES%" >nul
    if !errorlevel! equ 0 (
        echo [PASS] login-valid: 200 with a session cookie
    ) else (
        echo [FAIL] login-valid: got 200 but no JSESSIONID cookie was saved
        set /a FAILS+=1
    )
) else (
    echo [FAIL] login-valid: expected status 200, got !STATUS!
    set /a FAILS+=1
)

rem --- the same name from another session (no cookie) is rejected ---
for /f %%s in ('curl.exe -s -o "%TMP_BODY%" -w "%%{http_code}" -X POST "%BASE_URL%/login?username=%USERNAME%"') do set STATUS=%%s
set /p BODY=<"%TMP_BODY%"
if "!STATUS!"=="401" (
    echo !BODY! | findstr /c:"already taken" >nul
    if !errorlevel! equ 0 (
        echo [PASS] login-duplicate: 401 with the expected message
    ) else (
        echo [FAIL] login-duplicate: got 401 but the message was: !BODY!
        set /a FAILS+=1
    )
) else (
    echo [FAIL] login-duplicate: expected status 401, got !STATUS! ^(body: !BODY!^)
    set /a FAILS+=1
)

rem --- a missing name is rejected ---
for /f %%s in ('curl.exe -s -o "%TMP_BODY%" -w "%%{http_code}" -X POST "%BASE_URL%/login"') do set STATUS=%%s
set /p BODY=<"%TMP_BODY%"
if "!STATUS!"=="409" (
    echo !BODY! | findstr /c:"user name cannot be empty" >nul
    if !errorlevel! equ 0 (
        echo [PASS] login-missing-name: 409 with the expected message
    ) else (
        echo [FAIL] login-missing-name: got 409 but the message was: !BODY!
        set /a FAILS+=1
    )
) else (
    echo [FAIL] login-missing-name: expected status 409, got !STATUS! ^(body: !BODY!^)
    set /a FAILS+=1
)

rem --- a login changes data, so a login that is not sent as POST is refused ---
for /f %%s in ('curl.exe -s -o "%TMP_BODY%" -w "%%{http_code}" "%BASE_URL%/login?username=%USERNAME%_get"') do set STATUS=%%s
set /p BODY=<"%TMP_BODY%"
if "!STATUS!"=="400" (
    echo !BODY! | findstr /c:"must be sent as POST" >nul
    if !errorlevel! equ 0 (
        echo [PASS] login-not-post: 400 with the expected message
    ) else (
        echo [FAIL] login-not-post: got 400 but the message was: !BODY!
        set /a FAILS+=1
    )
) else (
    echo [FAIL] login-not-post: expected status 400, got !STATUS! ^(body: !BODY!^)
    set /a FAILS+=1
)

del /q "%TMP_BODY%" "%TMP_COOKIES%" >nul 2>nul
set RESULT=%FAILS%
endlocal
exit /b %RESULT%
