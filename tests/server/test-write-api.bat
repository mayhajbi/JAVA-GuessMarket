@echo off
rem ---------------------------------------------------------------------------
rem  Checks the write endpoints end to end (/event/create, /event/open,
rem  /event/buy, /event/order, /event/close): a wrong request gets 400 and the
rem  reason, and every balance is compared with a number computed from the LMSR
rem  formula and the order book rules. The checks themselves are in
rem  write-api-check.ps1 next to this file, which sends every request with
rem  curl.exe. Requires the guessmarket WAR to already be running locally on
rem  Tomcat (see ..\README.md) before this runs.
rem  Prints one [PASS]/[FAIL] line per check; exits with the number of
rem  failures (0 = all passed).
rem ---------------------------------------------------------------------------
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0write-api-check.ps1"
exit /b %errorlevel%
