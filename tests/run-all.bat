@echo off
rem ---------------------------------------------------------------------------
rem  Runs every check script under tests\ and reports the result: a summary
rem  in the terminal (X/Y individual checks passed, and the names of any that
rem  failed), and the full detail of every failure in tests\output.txt.
rem  Checks that a server-dependent script needs are only run when the server
rem  is actually reachable, so a downed server is never mistaken for a real
rem  check failure.
rem  To add a new check script: add one more "call :run <path> <name>" line
rem  below, next to the ones that already exist - inside the "if %SERVER_UP%"
rem  block if it talks to the server, outside it if it does not.
rem ---------------------------------------------------------------------------
setlocal enabledelayedexpansion
cd /d "%~dp0"

set BASE_URL=http://localhost:8080/guessmarket
set PASS_COUNT=0
set FAIL_COUNT=0
set FAILED_CHECKS=
set SKIPPED_CHECKS=
set OUTPUT_FILE=output.txt
if exist "%OUTPUT_FILE%" del /q "%OUTPUT_FILE%"

for /f %%s in ('curl.exe -s -o nul -w "%%{http_code}" --max-time 3 "%BASE_URL%/login" 2^>nul') do set PING_STATUS=%%s
if "%PING_STATUS%"=="000" (set SERVER_UP=0) else (set SERVER_UP=1)

if %SERVER_UP%==1 (
    call :run server\login.bat "server-login"
) else (
    echo Server not reachable at %BASE_URL% - start Tomcat first ^(see README.md^). Skipping: server-login
    set SKIPPED_CHECKS= server-login
)

set /a TOTAL_CHECKS=PASS_COUNT+FAIL_COUNT
echo.
if %FAIL_COUNT% GTR 0 (
    echo %PASS_COUNT%/%TOTAL_CHECKS% checks passed.
    echo Failed:%FAILED_CHECKS%
    echo See %OUTPUT_FILE% for the details of each failure.
) else if %TOTAL_CHECKS%==0 (
    echo No checks ran.
) else (
    echo All %PASS_COUNT%/%TOTAL_CHECKS% checks passed.
)
if not "%SKIPPED_CHECKS%"=="" echo Skipped ^(server not running^):%SKIPPED_CHECKS%
endlocal
goto :eof

:run
echo Running %~2...
set TMP_RUN_OUT=%TEMP%\gm_check_%RANDOM%.txt
call %~1 > "%TMP_RUN_OUT%" 2>&1
set RUN_RESULT=%ERRORLEVEL%
type "%TMP_RUN_OUT%"

for /f %%c in ('findstr /r /c:"^\[PASS\]" "%TMP_RUN_OUT%" ^| find /c /v ""') do set /a PASS_COUNT+=%%c
for /f %%c in ('findstr /r /c:"^\[FAIL\]" "%TMP_RUN_OUT%" ^| find /c /v ""') do set /a FAIL_COUNT+=%%c
for /f "tokens=1,* delims=]" %%a in ('findstr /r /c:"^\[FAIL\]" "%TMP_RUN_OUT%"') do (
    for /f "tokens=1 delims=:" %%n in ("%%b") do set FAILED_CHECKS=!FAILED_CHECKS! %%n
)

if not "%RUN_RESULT%"=="0" (
    echo ===== %~2 ===== >> "%OUTPUT_FILE%"
    type "%TMP_RUN_OUT%" >> "%OUTPUT_FILE%"
    echo. >> "%OUTPUT_FILE%"
)
del /q "%TMP_RUN_OUT%" >nul 2>nul
goto :eof
