@echo off
cd /d "%~dp0"
where mvn >nul 2>&1
if errorlevel 1 (
  echo Maven was not found. Install/configure Maven first.
  pause
  exit /b 1
)
mvn clean javafx:run
pause
