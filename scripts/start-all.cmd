@echo off
setlocal
cd /d "%~dp0.."

start "FROMS Python AI" cmd /k scripts\run-ai-service.cmd
start "FROMS Spring App" cmd /k scripts\run-app.cmd
