@echo off
rem ---------------------------------------------------------------------------
rem  Checks the /upload endpoint: refused without a session (401); a valid file
rem  adds its event (200, the event name in the answer); the same file again, a
rem  broken file, a request with no file, a request that is not multipart and a
rem  file over 1MB are all 400 with a message; and the upload leaves no new file
rem  in the webapps / work / temp folders of the Tomcat that IntelliJ runs (the
rem  default paths below; CATALINA_BASE / CATALINA_HOME override them) nor in the
rem  temp folder of Tomcat itself. Each run
rem  uploads an event with a fresh name, so it does not depend on what is already
rem  in the server. Requires the guessmarket WAR to already be running locally on
rem  Tomcat (see ..\README.md) before this runs.
rem  Prints one [PASS]/[FAIL] line per check; exits with the number of
rem  failures (0 = all passed).
rem ---------------------------------------------------------------------------
setlocal enabledelayedexpansion

set BASE_URL=http://localhost:8080/guessmarket
set GM_USER=upload_%RANDOM%
set EVENT_NAME=Upload check %RANDOM%%RANDOM%
set FAILS=0
set TOMCAT_BASE=%CATALINA_BASE%
if "%TOMCAT_BASE%"=="" set TOMCAT_BASE=C:\Users\mayha\AppData\Local\JetBrains\IntelliJIdea2026.2\tomcat\75a9e264-f35b-4f11-b0da-f28176c09c55
set TOMCAT_HOME=%CATALINA_HOME%
if "%TOMCAT_HOME%"=="" set TOMCAT_HOME=C:\Users\mayha\tools\apache-tomcat-10.1.26
set BODY_FILE=%TEMP%\gm_upload_body.txt
set COOKIES=%TEMP%\gm_upload_cookies.txt
set GOOD_XML=%TEMP%\gm_upload_good.xml
set BROKEN_XML=%TEMP%\gm_upload_broken.xml
set BIG_XML=%TEMP%\gm_upload_big.xml
if exist "%COOKIES%" del /q "%COOKIES%"

rem --- the files to upload: a valid one with a fresh event name, a broken one and one over 1MB ---
powershell -NoProfile -Command "(Get-Content -Raw -Encoding UTF8 '%~dp0..\data\ex3\small.xml').Replace('Mujtaba is Dead','%EVENT_NAME%') | Set-Content -Encoding UTF8 '%GOOD_XML%'"
echo ^<Guess-Market^>^<GM-events^> > "%BROKEN_XML%"
powershell -NoProfile -Command "[IO.File]::WriteAllBytes('%BIG_XML%', (New-Object byte[] 1200000))"

rem --- no session ---
call :expect "no-session" 401 "" -F "file=@%GOOD_XML%"

curl.exe -s -o nul -c "%COOKIES%" "%BASE_URL%/login?username=%GM_USER%"

rem --- the disk before the upload ---
call :count_files BEFORE

rem --- a valid file adds its event ---
call :expect "upload-valid" 200 "%EVENT_NAME%" -b "%COOKIES%" -F "file=@%GOOD_XML%"

rem --- the disk after the upload ---
call :count_files AFTER
if "!BEFORE!"=="skip" (
    echo [SKIP] upload-no-disk-file: the Tomcat folders were not found, check them by hand
) else if "!BEFORE!"=="!AFTER!" (
    echo [PASS] upload-no-disk-file: webapps / work / temp hold !AFTER! files, before and after
) else (
    echo [FAIL] upload-no-disk-file: files before !BEFORE!, after !AFTER!
    set /a FAILS+=1
)

rem --- every wrong upload gets 400 and a message ---
call :expect "upload-same-file-again" 400 "already in use" -b "%COOKIES%" -F "file=@%GOOD_XML%"
call :expect "upload-broken-file" 400 "Reason:" -b "%COOKIES%" -F "file=@%BROKEN_XML%"
call :expect "upload-too-big" 400 "too big" -b "%COOKIES%" -F "file=@%BIG_XML%"
call :expect "upload-no-file" 400 "no file" -b "%COOKIES%" -F "note=hello"
call :expect "upload-not-multipart" 400 "multipart" -b "%COOKIES%" -d "hello"

del /q "%BODY_FILE%" "%COOKIES%" "%GOOD_XML%" "%BROKEN_XML%" "%BIG_XML%" >nul 2>nul
set RESULT=%FAILS%
endlocal & exit /b %RESULT%

rem  :expect <name> <status> <text the answer must contain, or "" for any non-empty one> <curl arguments...>
:expect
set NAME=%~1
set WANT=%~2
set TEXT=%~3
shift & shift & shift
for /f %%s in ('curl.exe -s -o "%BODY_FILE%" -w "%%{http_code}" -X POST %1 %2 %3 %4 %5 %6 "%BASE_URL%/upload"') do set STATUS=%%s
if not "!STATUS!"=="!WANT!" (
    echo [FAIL] !NAME!: expected status !WANT!, got !STATUS!
    set /a FAILS+=1
    exit /b
)
if "!TEXT!"=="" (
    for %%z in ("%BODY_FILE%") do set SIZE=%%~zz
    if "!SIZE!"=="0" (
        echo [FAIL] !NAME!: status !WANT! but the answer is empty
        set /a FAILS+=1
    ) else (
        echo [PASS] !NAME!: !WANT! with a message
    )
    exit /b
)
findstr /i /c:"!TEXT!" "%BODY_FILE%" >nul
if !errorlevel! equ 0 (
    echo [PASS] !NAME!: !WANT! with "!TEXT!"
) else (
    echo [FAIL] !NAME!: status !WANT! but the answer has no "!TEXT!"
    set /a FAILS+=1
)
exit /b

rem  :count_files <variable> - the number of files under webapps, work and temp of the Tomcat base and the temp of its home, or "skip"
:count_files
if not exist "%TOMCAT_BASE%\work" (
    set %~1=skip
    exit /b
)
for /f %%c in ('powershell -NoProfile -Command "(Get-ChildItem -Recurse -File '%TOMCAT_BASE%\webapps','%TOMCAT_BASE%\work','%TOMCAT_BASE%\temp','%TOMCAT_HOME%\temp' -ErrorAction SilentlyContinue).Count"') do set %~1=%%c
exit /b
