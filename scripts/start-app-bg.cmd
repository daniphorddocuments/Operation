@echo off
setlocal
start "mahanga-app" /b cmd /c "call C:\PROJECT\mahanga\scripts\run-app.cmd > C:\PROJECT\mahanga\app.log 2>&1"
