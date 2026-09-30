@echo off
rem ---------------------------------------------------------------------------
rem  Compiles the dto and engine sources together with the engine checks under
rem  tests\engine (they use the gson jar of the server) into %TEMP%\gm-tests\classes, straight from the sources, so the
rem  checks always run against the current code without a build.bat run or
rem  anything done in IntelliJ, and build.bat's out\ and jars\ folders are not
rem  touched. Needs javac (JDK 25) on the PATH.
rem  Prints one [PASS]/[FAIL] line; exits with 0 when everything compiled.
rem ---------------------------------------------------------------------------
setlocal enabledelayedexpansion
cd /d "%~dp0"
for %%i in ("%~dp0..") do set ROOT=%%~fi
set WORK=%TEMP%\gm-tests
set CLASSES=%WORK%\classes
set SOURCES=%WORK%\sources.txt
set JAVAC_OUT=%WORK%\javac.txt

where javac >nul 2>nul
if errorlevel 1 (
    echo [FAIL] compile: javac was not found on the PATH - add the bin folder of JDK 25 to it
    exit /b 1
)

if exist "%CLASSES%" rmdir /s /q "%CLASSES%"
mkdir "%CLASSES%"
rem The argument file gets one quoted path with forward slashes per line (javac reads backslashes in it as escapes).
(for /f "delims=" %%f in ('dir /s /b "%ROOT%\dto\src\*.java" "%ROOT%\engine\src\*.java" engine\*.java') do (
    set "SOURCE=%%f"
    echo "!SOURCE:\=/!"
)) > "%SOURCES%"

javac -encoding UTF-8 -cp "%ROOT%\lib\*;%ROOT%\server\lib\*" -d "%CLASSES%" @"%SOURCES%" > "%JAVAC_OUT%" 2>&1
if errorlevel 1 (
    echo [FAIL] compile: the dto, engine and engine checks sources did not compile
    type "%JAVAC_OUT%"
    exit /b 1
)
echo [PASS] compile: dto, engine and engine checks
exit /b 0
