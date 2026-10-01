@echo off
rem ---------------------------------------------------------------------------
rem  Builds the client of Guess Market (exercise 3): compiles the dto, api,
rem  engine, ui-fx and client-fx modules and creates the client under
rem  "jars\client" - one jar for each module the client needs (dto, api, ui-fx
rem  and client-fx), the third party libraries under lib, and run-client.bat.
rem  The engine is compiled here only to check it; it runs on the server, inside
rem  the WAR that IntelliJ builds, so it is not part of the client.
rem  Requires JDK 25 (javac and jar) to be available in the PATH. JavaFX is
rem  bundled under lib\javafx-sdk-22.0.2.
rem ---------------------------------------------------------------------------
setlocal
cd /d "%~dp0"

set FX=lib\javafx-sdk-22.0.2\lib

rem Only the folders this script creates are cleaned: the WAR that IntelliJ builds under out\artifacts stays.
for %%d in (dto api engine ui-fx client-fx) do if exist out\%%d rmdir /s /q out\%%d
if exist jars\client rmdir /s /q jars\client
mkdir out\dto
mkdir out\api
mkdir out\engine
mkdir out\ui-fx
mkdir out\client-fx
mkdir jars\client

echo [1/5] Compiling the dto module...
dir /s /b dto\src\*.java > out\sources-dto.txt
javac -encoding UTF-8 -d out\dto @out\sources-dto.txt
if errorlevel 1 goto failed

echo [2/5] Compiling the api module...
dir /s /b api\src\*.java > out\sources-api.txt
javac -encoding UTF-8 -cp "out\dto" -d out\api @out\sources-api.txt
if errorlevel 1 goto failed

echo [3/5] Compiling the engine module...
dir /s /b engine\src\*.java > out\sources-engine.txt
javac -encoding UTF-8 -cp "out\dto;out\api;lib\*" -d out\engine @out\sources-engine.txt
if errorlevel 1 goto failed

echo [4/5] Compiling the ui-fx module (JavaFX)...
dir /s /b ui-fx\src\*.java > out\sources-ui-fx.txt
javac -encoding UTF-8 --module-path "%FX%" --add-modules javafx.controls,javafx.fxml ^
      -cp "out\dto;out\api" -d out\ui-fx @out\sources-ui-fx.txt
if errorlevel 1 goto failed
rem The screens are described in FXML files with their style sheets, loaded from the jar at run time.
xcopy /s /y /q ui-fx\src\*.fxml out\ui-fx\ >nul
if errorlevel 1 goto failed
xcopy /s /y /q ui-fx\src\*.css out\ui-fx\ >nul
if errorlevel 1 goto failed

echo [5/5] Compiling the client-fx module (JavaFX, OkHttp, Gson)...
dir /s /b client-fx\src\*.java > out\sources-client-fx.txt
javac -encoding UTF-8 --module-path "%FX%" --add-modules javafx.controls,javafx.fxml ^
      -cp "out\dto;out\api;out\ui-fx;lib\okhttp\*;lib\gson\*" -d out\client-fx @out\sources-client-fx.txt
if errorlevel 1 goto failed
xcopy /s /y /q client-fx\src\*.fxml out\client-fx\ >nul
if errorlevel 1 goto failed

echo Creating the jar files...
jar --create --file jars\client\gm-dto.jar -C out\dto .
if errorlevel 1 goto failed
jar --create --file jars\client\gm-api.jar -C out\api .
if errorlevel 1 goto failed
jar --create --file jars\client\gm-ui-fx.jar -C out\ui-fx .
if errorlevel 1 goto failed
jar --create --file jars\client\gm-client-fx.jar --main-class gm.client.GuessMarketClientApp -C out\client-fx .
if errorlevel 1 goto failed

mkdir jars\client\lib
xcopy /e /i /q /y lib\okhttp jars\client\lib\okhttp >nul
xcopy /e /i /q /y lib\gson jars\client\lib\gson >nul
xcopy /e /i /q /y lib\javafx-sdk-22.0.2 jars\client\lib\javafx-sdk-22.0.2 >nul
copy /y run-client.bat jars\client\ >nul

echo.
echo Build finished successfully. Run the client with: jars\client\run-client.bat
goto end

:failed
echo.
echo Build failed. Please read the compilation errors above.

:end
endlocal
