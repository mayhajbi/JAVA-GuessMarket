@echo off
rem ---------------------------------------------------------------------------
rem  Checks the automatic updates of the client against the running server,
rem  with the classes of the client itself (Refresher, Query, HttpApi): a
rem  client that is not logged in is told so (401), a logged in client gets its
rem  data, what another user does reaches it within 2 seconds, and nothing is
rem  pulled while the updates are turned off. The checks themselves are in
rem  LivePullCheck.java next to this file, compiled from the sources by
rem  ..\client\run-check.bat. Requires the guessmarket WAR to already be
rem  running locally on Tomcat (see ..\README.md) before this runs.
rem  Prints one [PASS]/[FAIL] line; exits with 0 when everything held.
rem ---------------------------------------------------------------------------
call "%~dp0..\client\run-check.bat" server\LivePullCheck.java
exit /b %errorlevel%
