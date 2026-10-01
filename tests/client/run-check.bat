@echo off
rem ---------------------------------------------------------------------------
rem  Compiles and runs one check of the client, given by the path of its source
rem  file relative to the tests folder. Usage: run-check.bat client\<Name>Test.java
rem  The check and every client class it uses are compiled straight from the
rem  sources (dto, api, ui-fx and client-fx) into %TEMP%\gm-tests\client-classes,
rem  so it always checks the current code without a build.bat run. The FXML
rem  and CSS files are read from the source folders. Needs javac and java
rem  (JDK 25) on the PATH.
rem  The check prints one [PASS]/[FAIL] line; this exits with the check's own
rem  code (0 = everything held).
rem ---------------------------------------------------------------------------
setlocal
cd /d "%~dp0.."
for %%i in ("%~dp0..\..") do set ROOT=%%~fi
set CLASSES=%TEMP%\gm-tests\client-classes
set JAVAC_OUT=%TEMP%\gm-tests\client-javac.txt
set FX=%ROOT%\lib\javafx-sdk-22.0.2\lib
set LIBS=%ROOT%\lib\okhttp\*;%ROOT%\lib\gson\*
set CHECK=%~n1

if exist "%CLASSES%" rmdir /s /q "%CLASSES%"
mkdir "%CLASSES%"

javac -encoding UTF-8 --module-path "%FX%" --add-modules javafx.controls,javafx.fxml -cp "%LIBS%" ^
      -sourcepath "%ROOT%\dto\src;%ROOT%\api\src;%ROOT%\ui-fx\src;%ROOT%\client-fx\src;engine" ^
      -d "%CLASSES%" "%~1" > "%JAVAC_OUT%" 2>&1
if errorlevel 1 (
    echo [FAIL] %CHECK%: the check and the client sources it uses did not compile
    type "%JAVAC_OUT%"
    exit /b 1
)

java --module-path "%FX%" --add-modules javafx.controls,javafx.fxml ^
     -cp "%CLASSES%;%ROOT%\ui-fx\src;%ROOT%\client-fx\src;%LIBS%" %CHECK%
exit /b %ERRORLEVEL%
