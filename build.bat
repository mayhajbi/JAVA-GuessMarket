@echo off
rem ---------------------------------------------------------------------------
rem  Builds the four modules of Guess Market into four separate jar files.
rem  The result is created under the "jars" folder, which is exactly what needs
rem  to be submitted (the jars, the third party libraries under lib, and the
rem  run scripts).
rem  Requires JDK 25 (javac and jar) to be available in the PATH. JavaFX is
rem  bundled under lib\javafx-sdk-22.0.2.
rem ---------------------------------------------------------------------------
setlocal
cd /d "%~dp0"

set FX=lib\javafx-sdk-22.0.2\lib

if exist out rmdir /s /q out
if exist jars rmdir /s /q jars
mkdir out\dto
mkdir out\engine
mkdir out\ui
mkdir out\ui-fx
mkdir jars\lib

echo [1/4] Compiling the dto module...
dir /s /b dto\src\*.java > out\sources-dto.txt
javac -encoding UTF-8 -d out\dto @out\sources-dto.txt
if errorlevel 1 goto failed

echo [2/4] Compiling the engine module...
dir /s /b engine\src\*.java > out\sources-engine.txt
javac -encoding UTF-8 -cp "out\dto;lib\*" -d out\engine @out\sources-engine.txt
if errorlevel 1 goto failed

echo [3/4] Compiling the ui module (console)...
dir /s /b ui\src\*.java > out\sources-ui.txt
javac -encoding UTF-8 -cp "out\dto;out\engine" -d out\ui @out\sources-ui.txt
if errorlevel 1 goto failed

echo [4/4] Compiling the ui-fx module (JavaFX)...
dir /s /b ui-fx\src\*.java > out\sources-ui-fx.txt
javac -encoding UTF-8 --module-path "%FX%" --add-modules javafx.controls,javafx.fxml ^
      -cp "out\dto;out\engine" -d out\ui-fx @out\sources-ui-fx.txt
if errorlevel 1 goto failed

echo Creating the jar files...
jar --create --file jars\gm-dto.jar -C out\dto .
if errorlevel 1 goto failed
jar --create --file jars\gm-engine.jar -C out\engine .
if errorlevel 1 goto failed
jar --create --file jars\gm-ui.jar --main-class gm.ui.GuessMarketConsoleApp -C out\ui .
if errorlevel 1 goto failed
jar --create --file jars\gm-ui-fx.jar --main-class gm.ui.fx.GuessMarketApp -C out\ui-fx .
if errorlevel 1 goto failed

copy /y lib\*.jar jars\lib\ >nul 2>nul
xcopy /e /i /q /y lib\javafx-sdk-22.0.2 jars\lib\javafx-sdk-22.0.2 >nul
copy /y run.bat jars\ >nul
copy /y run-console.bat jars\ >nul

echo.
echo Build finished successfully. Run the system with: jars\run.bat
goto end

:failed
echo.
echo Build failed. Please read the compilation errors above.

:end
endlocal
