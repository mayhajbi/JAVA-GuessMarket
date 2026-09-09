@echo off
rem ---------------------------------------------------------------------------
rem  Runs the Guess Market JavaFX application (exercise 2).
rem  Works both from the project folder (after build.bat) and from the folder
rem  that holds the jar files themselves.
rem  Requires JDK 25 to be available in the PATH. JavaFX is bundled under
rem  lib\javafx-sdk-22.0.2 and is not needed anywhere on the machine.
rem ---------------------------------------------------------------------------
setlocal
cd /d "%~dp0"
if exist jars\gm-ui-fx.jar cd jars

set FX=lib\javafx-sdk-22.0.2
set PATH=%CD%\%FX%\bin;%PATH%

java --module-path "%FX%\lib" --add-modules javafx.controls,javafx.fxml ^
     --enable-native-access=javafx.graphics ^
     -cp "gm-ui-fx.jar;gm-engine.jar;gm-dto.jar;lib\*" gm.ui.fx.GuessMarketApp
endlocal
