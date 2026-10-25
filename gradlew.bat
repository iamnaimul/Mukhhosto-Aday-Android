@echo off
setlocal
set VERSION=8.7
if "%GRADLE_USER_HOME%"=="" set GRADLE_USER_HOME=%USERPROFILE%\.gradle
set BASE=%GRADLE_USER_HOME%\manual-wrapper
set DIST=%BASE%\gradle-%VERSION%
if exist "%DIST%\bin\gradle.bat" goto run
if not exist "%BASE%" mkdir "%BASE%"
set ZIP=%BASE%\gradle-%VERSION%-bin.zip
if not exist "%ZIP%" (
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-%VERSION%-bin.zip' -OutFile '%ZIP%'"
  if errorlevel 1 exit /b 1
)
powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -Path '%ZIP%' -DestinationPath '%BASE%' -Force"
if errorlevel 1 exit /b 1
:run
call "%DIST%\bin\gradle.bat" %*
exit /b %ERRORLEVEL%
