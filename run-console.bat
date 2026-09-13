@echo off
rem ---------------------------------------------------------------------------
rem  Runs the Guess Market console application (exercise 1).
rem  The JavaFX application of exercise 2 is started by run.bat instead.
rem  DISABLED: the console belongs to the previous implementation. It does not
rem  compile against the engine of exercise 2 (every action now needs a user),
rem  so build.bat does not create gm-ui.jar. The command that ran it is kept
rem  below for reference.
rem ---------------------------------------------------------------------------
echo The console application of exercise 1 is disabled: it does not compile against
echo the engine of exercise 2. Run the JavaFX application with run.bat instead.
exit /b 1

setlocal
cd /d "%~dp0"
if exist jars\gm-ui.jar cd jars

java -cp "gm-ui.jar;gm-engine.jar;gm-dto.jar;lib\*" gm.ui.GuessMarketConsoleApp
endlocal
