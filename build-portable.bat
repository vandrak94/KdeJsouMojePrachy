@echo off
setlocal EnableExtensions
cd /d "%~dp0"
call "%~dp0build-exe.bat"
if errorlevel 1 exit /b 1
set "APP_NAME=KdeJsouMojePrachy"
where powershell >nul 2>nul || (echo ERROR: PowerShell is not in PATH. & exit /b 1)
powershell -NoProfile -ExecutionPolicy Bypass -Command "Compress-Archive -LiteralPath 'dist\%APP_NAME%' -DestinationPath 'dist\%APP_NAME%-portable.zip' -Force"
if errorlevel 1 exit /b 1
echo Built: dist\%APP_NAME%-portable.zip
exit /b 0
