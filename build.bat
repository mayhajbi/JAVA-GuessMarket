@echo off
rem ---------------------------------------------------------------------------
rem  Builds the three modules of Guess Market into three separate jar files.
rem  The result is created under the "jars" folder, which is exactly what needs
rem  to be submitted (three jars, the third party jars under lib, and run.bat).
rem  Requires JDK 25 (javac and jar) to be available in the PATH.
rem ---------------------------------------------------------------------------
setlocal
cd /d "%~dp0"

if exist out rmdir /s /q out
if exist jars rmdir /s /q jars
mkdir out\dto
mkdir out\engine
mkdir out\ui
mkdir jars\lib

echo [1/3] Compiling the dto module...
dir /s /b dto\src\*.java > out\sources-dto.txt
javac -encoding UTF-8 -d out\dto @out\sources-dto.txt
if errorlevel 1 goto failed

echo [2/3] Compiling the engine module...
dir /s /b engine\src\*.java > out\sources-engine.txt
javac -encoding UTF-8 -cp "out\dto;lib\*" -d out\engine @out\sources-engine.txt
if errorlevel 1 goto failed

echo [3/3] Compiling the ui module...
dir /s /b ui\src\*.java > out\sources-ui.txt
javac -encoding UTF-8 -cp "out\dto;out\engine" -d out\ui @out\sources-ui.txt
if errorlevel 1 goto failed

echo Creating the jar files...
jar --create --file jars\gm-dto.jar -C out\dto .
if errorlevel 1 goto failed
jar --create --file jars\gm-engine.jar -C out\engine .
if errorlevel 1 goto failed
jar --create --file jars\gm-ui.jar --main-class gm.ui.GuessMarketConsoleApp -C out\ui .
if errorlevel 1 goto failed

copy /y lib\*.jar jars\lib\ >nul 2>nul
copy /y run.bat jars\ >nul

echo.
echo Build finished successfully. Run the system with: jars\run.bat
goto end

:failed
echo.
echo Build failed. Please read the compilation errors above.

:end
endlocal
