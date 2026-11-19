@echo off
setlocal
cd /d "%~dp0"

set "LOG=%~dp0run-dev.log"

where mvn >nul 2>nul
if errorlevel 1 (
    echo Maven nebyl nalezen v PATH.
    echo Maven nebyl nalezen v PATH. > "%LOG%"
    pause
    exit /b 1
)

rem Capture the complete Maven output without PowerShell or mixed encodings.
call mvn clean compile exec:java > "%LOG%" 2>&1
set "RESULT=%ERRORLEVEL%"
type "%LOG%"

if not "%RESULT%"=="0" (
    echo.
    echo Kompilace nebo spusteni selhalo. Log: "%LOG%"
    pause
    exit /b %RESULT%
)

exit /b 0
