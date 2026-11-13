@echo off
setlocal EnableExtensions EnableDelayedExpansion
cd /d "%~dp0"

where mvn >nul 2>nul || (echo ERROR: Maven is not in PATH. & exit /b 1)
where jar >nul 2>nul || (echo ERROR: JDK jar is not in PATH. & exit /b 1)
where jpackage >nul 2>nul || (echo ERROR: JDK jpackage is not in PATH. & exit /b 1)
where powershell >nul 2>nul || (echo ERROR: PowerShell is not in PATH. & exit /b 1)
if not exist "src\main\resources\app.ico" (echo ERROR: Missing src\main\resources\app.ico & exit /b 1)
if not exist "src\main\resources\app-icon.png" (echo ERROR: Missing src\main\resources\app-icon.png & exit /b 1)
if not exist "licenses\collect-licenses.ps1" (echo ERROR: Missing licenses\collect-licenses.ps1 & exit /b 1)

set "APP_NAME=KdeJsouMojePrachy"
set "INPUT=target\package-input"
set "OUTPUT=dist"

call mvn -B clean compile dependency:copy-dependencies "-DoutputDirectory=target/package-input/lib" "-DincludeScope=runtime"
if errorlevel 1 exit /b 1

> "target\app-manifest.mf" echo Manifest-Version: 1.0
>> "target\app-manifest.mf" echo Main-Class: KdeJsouMojePrachyApp
set "CP="
for %%F in ("%INPUT%\lib\*.jar") do (
    if exist "%%~fF" set "CP=!CP! lib/%%~nxF"
)
if not defined CP (echo ERROR: No runtime dependencies were copied. & exit /b 1)
>> "target\app-manifest.mf" echo Class-Path:!CP!
>> "target\app-manifest.mf" echo.
jar cfm "%INPUT%\app.jar" "target\app-manifest.mf" -C "target\classes" .
if errorlevel 1 exit /b 1

if not exist "%OUTPUT%" mkdir "%OUTPUT%"
if exist "%OUTPUT%\%APP_NAME%" rmdir /s /q "%OUTPUT%\%APP_NAME%"
if exist "%OUTPUT%\%APP_NAME%" (echo ERROR: Could not remove previous app-image. & exit /b 1)

jpackage --type app-image --name "%APP_NAME%" --input "%INPUT%" --main-jar app.jar --main-class KdeJsouMojePrachyApp --dest "%OUTPUT%" --app-version 1.0.0 --icon "src\main\resources\app.ico"
if errorlevel 1 exit /b 1

if not exist "%OUTPUT%\%APP_NAME%\data" mkdir "%OUTPUT%\%APP_NAME%\data"
if exist "data\database.db" copy /y "data\database.db" "%OUTPUT%\%APP_NAME%\data\database.db" >nul

powershell -NoProfile -ExecutionPolicy Bypass -File "licenses\collect-licenses.ps1" -AppDir "%CD%\%OUTPUT%\%APP_NAME%" -JarDir "%CD%\%INPUT%\lib" -ProjectLicenses "%CD%\licenses"
if errorlevel 1 (echo ERROR: Could not collect license notices. & exit /b 1)

echo Built: %OUTPUT%\%APP_NAME%\%APP_NAME%.exe
exit /b 0
