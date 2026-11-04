@echo off
cd /d "%~dp0"
powershell -NoProfile -ExecutionPolicy Bypass -File "scripts\start-local.ps1"
if errorlevel 1 (
  echo.
  echo Startup failed. Press any key to exit.
  pause >nul
  exit /b 1
)
start "" "http://127.0.0.1:5173"
