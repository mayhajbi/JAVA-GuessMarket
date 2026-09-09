@echo off
rem ---------------------------------------------------------------------------
rem  Runs the Guess Market console application (exercise 1).
rem  The JavaFX application of exercise 2 is started by run.bat instead.
rem  Works both from the project folder (after build.bat) and from the folder
rem  that holds the jar files themselves.
rem  Requires JRE/JDK 25 to be available in the PATH.
rem ---------------------------------------------------------------------------
setlocal
cd /d "%~dp0"
if exist jars\gm-ui.jar cd jars

java -cp "gm-ui.jar;gm-engine.jar;gm-dto.jar;lib\*" gm.ui.GuessMarketConsoleApp
endlocal
