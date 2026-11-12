@echo off
setlocal EnableExtensions EnableDelayedExpansion
cd /d "%~dp0"

where mvn >nul 2>nul || (echo ERROR: Maven is not in PATH. & exit /b 1)
where jar >nul 2>nul || (echo ERROR: JDK jar is not in PATH. & exit /b 1)
where jpackage >nul 2>nul || (echo ERROR: JDK jpackage is not in PATH. & exit /b 1)

set "APP_NAME=KdeJsouMojePrachy"
set "INPUT=target\package-input"
set "OUTPUT=dist"

call mvn -B clean compile dependency:copy-dependencies "-DoutputDirectory=target/package-input/lib" "-DincludeScope=runtime"
if errorlevel 1 exit /b 1

rem Create a JAR manifest with the runtime dependency classpath.
> "target\app-manifest.mf" echo Manifest-Version: 1.0
>> "target\app-manifest.mf" echo Main-Class: KdeJsouMojePrachyApp
set "CP="
for %%F in ("%INPUT%\lib\*.jar") do (
    if exist "%%~fF" set "CP=!CP! lib/%%~nxF"
)
if not defined CP (echo ERROR: No dependency JARs were copied. & exit /b 1)
>> "target\app-manifest.mf" echo Class-Path:!CP!
>> "target\app-manifest.mf" echo.

jar cfm "%INPUT%\app.jar" "target\app-manifest.mf" -C "target\classes" .
if errorlevel 1 exit /b 1

if not exist "%OUTPUT%" mkdir "%OUTPUT%"
if exist "%OUTPUT%\%APP_NAME%" rmdir /s /q "%OUTPUT%\%APP_NAME%"

jpackage --type app-image --name "%APP_NAME%" --input "%INPUT%" --main-jar app.jar --main-class KdeJsouMojePrachyApp --dest "%OUTPUT%" --app-version 1.0.0 --win-console
if errorlevel 1 exit /b 1

rem Keep the database writable next to the launcher, outside the bundled JAR.
if not exist "%OUTPUT%\%APP_NAME%\data" mkdir "%OUTPUT%\%APP_NAME%\data"
if not exist "%OUTPUT%\%APP_NAME%\data\database.db" (
    if exist "data\database.db" copy /y "data\database.db" "%OUTPUT%\%APP_NAME%\data\database.db" >nul
)

echo.
echo Built: %OUTPUT%\%APP_NAME%\%APP_NAME%.exe
echo NOTE: The database path is relative to the current working directory.
echo Launch from the application directory to use its data\database.db.
exit /b 0
