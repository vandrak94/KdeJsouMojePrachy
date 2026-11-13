@echo off
setlocal
cd /d "%~dp0"
where mvn >nul 2>nul
if errorlevel 1 (
    echo Maven nebyl nalezen v PATH.
    pause
    exit /b 1
)
mvn clean compile exec:java
if errorlevel 1 pause
