@echo off
rem ---------------------------------------------------------------------------
rem  Runs the Guess Market client (exercise 3). The server has to be running.
rem  Works both from the project folder (after build.bat) and from the folder
rem  that holds the jar files themselves.
rem  Requires JDK 25 to be available in the PATH. JavaFX is bundled under
rem  lib\javafx-sdk-22.0.2 and is not needed anywhere on the machine.
rem ---------------------------------------------------------------------------
setlocal
cd /d "%~dp0"
if exist jars\client\gm-client-fx.jar cd jars\client

set FX=lib\javafx-sdk-22.0.2
set PATH=%CD%\%FX%\bin;%PATH%

java --module-path "%FX%\lib" --add-modules javafx.controls,javafx.fxml ^
     --enable-native-access=javafx.graphics ^
     -cp "gm-client-fx.jar;gm-ui-fx.jar;gm-api.jar;gm-dto.jar;lib\okhttp\*;lib\gson\*" gm.client.GuessMarketClientApp
endlocal
