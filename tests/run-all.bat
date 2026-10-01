@echo off
rem ---------------------------------------------------------------------------
rem  Runs every check script under tests\ and reports the result: a summary
rem  in the terminal (X/Y individual checks passed, and the names of any that
rem  failed), and the full detail of every failure in tests\output.txt.
rem  Checks that a server-dependent script needs are only run when the server
rem  is actually reachable, so a downed server is never mistaken for a real
rem  check failure.
rem  The scripts are found by their names, so a new check needs no edit here:
rem    engine\<Name>Test.java  - an engine check class (see engine\Check.java)
rem    client\<Name>Test.java  - a check of the client that needs no server
rem    server\test-<name>.bat   - a check that talks to the running server
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

rem The engine checks compile straight from the sources, so they need javac. When it is not on the PATH,
rem the JDK 25 folder of this machine is tried.
where javac >nul 2>nul
if errorlevel 1 if exist "C:\Program Files\Java\jdk-25.0.4\bin\javac.exe" set "PATH=C:\Program Files\Java\jdk-25.0.4\bin;%PATH%"

call :run .\compile.bat "compile"
set ENGINE_READY=0
if "%RUN_RESULT%"=="0" set ENGINE_READY=1
if %ENGINE_READY%==1 (
    for %%f in (engine\*Test.java) do call :run "engine\run-check.bat %%~nf" "engine-%%~nf"
) else (
    echo Skipping the engine checks - the sources did not compile.
    set SKIPPED_CHECKS=!SKIPPED_CHECKS! engine-checks
)

for %%f in (client\*Test.java) do call :run "client\run-check.bat %%f" "client-%%~nf"

if %SERVER_UP%==1 (
    for %%f in (server\test-*.bat) do call :run "%%f" "server-%%~nf"
) else (
    echo Server not reachable at %BASE_URL% - start Tomcat first ^(see README.md^). Skipping the server checks
    set SKIPPED_CHECKS=!SKIPPED_CHECKS! server-checks
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
if not "%SKIPPED_CHECKS%"=="" echo Skipped:%SKIPPED_CHECKS%
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
