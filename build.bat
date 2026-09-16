@echo off
setlocal
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0build.ps1" %*
set "RESULT=%ERRORLEVEL%"
endlocal & exit /b %RESULT%
