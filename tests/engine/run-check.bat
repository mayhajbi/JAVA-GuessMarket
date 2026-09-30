@echo off
rem ---------------------------------------------------------------------------
rem  Runs one engine check class, given by name, from the classes that
rem  ..\compile.bat built. Usage: run-check.bat <CheckClassName>
rem  The check prints one [PASS]/[FAIL] line; this exits with the check's own
rem  code (0 = everything held).
rem ---------------------------------------------------------------------------
setlocal
cd /d "%~dp0.."
for %%i in ("%~dp0..\..") do set ROOT=%%~fi
java -cp "%TEMP%\gm-tests\classes;%ROOT%\lib\*" %1
exit /b %ERRORLEVEL%
