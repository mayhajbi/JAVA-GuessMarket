@echo off
rem ---------------------------------------------------------------------------
rem  Checks the WAR file that IntelliJ builds, which is what Tomcat runs: it
rem  holds the jar of every module the server needs and every third party
rem  library, it does not hold the servlet API, and it was built after the last
rem  change of the sources inside it (otherwise the server checks run against
rem  old code). The checks themselves are in war-check.ps1 next to this file,
rem  because comparing dates and reading a zip cannot be done in a plain bat.
rem  Prints one [PASS]/[FAIL] line per check; exits with the number of
rem  failures (0 = all passed).
rem ---------------------------------------------------------------------------
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0war-check.ps1"
exit /b %errorlevel%
