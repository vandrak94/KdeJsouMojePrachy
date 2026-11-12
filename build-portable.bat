@echo off
setlocal EnableExtensions
cd /d "%~dp0"

call "%~dp0build-exe.bat"
if errorlevel 1 exit /b 1

set "APP_NAME=KdeJsouMojePrachy"
set "ZIP_PATH=%CD%\dist\%APP_NAME%-portable.zip"

where powershell >nul 2>nul || (echo ERROR: PowerShell is not available. & exit /b 1)
powershell -NoProfile -ExecutionPolicy Bypass -Command "Compress-Archive -LiteralPath 'dist\%APP_NAME%' -DestinationPath '%ZIP_PATH%' -Force"
if errorlevel 1 exit /b 1

echo.
echo Built: %ZIP_PATH%
exit /b 0
