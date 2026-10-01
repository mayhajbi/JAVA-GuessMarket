@echo off
rem ---------------------------------------------------------------------------
rem  Checks the chat (bonus) against the running server, with the classes of
rem  the client itself (HttpApi, HttpGuessMarketEngine): refused without a
rem  session, a line of one user reaches another user as the only new line, the
rem  writer is the user of the session, and a line that is empty or not in
rem  English is refused with the reason. The checks themselves are in
rem  ChatCheck.java next to this file, compiled from the sources by
rem  ..\client\run-check.bat. Requires the guessmarket WAR to already be
rem  running locally on Tomcat (see ..\README.md) before this runs.
rem  Prints one [PASS]/[FAIL] line; exits with 0 when everything held.
rem ---------------------------------------------------------------------------
call "%~dp0..\client\run-check.bat" server\ChatCheck.java
exit /b %errorlevel%
