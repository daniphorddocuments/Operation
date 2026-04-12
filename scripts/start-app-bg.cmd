@echo off
setlocal
cd /d "%~dp0.."
start "froms-app" /b cmd /c "call scripts\run-app.cmd > app.log 2>&1"
