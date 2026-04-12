@echo off
setlocal
cd /d "%~dp0.."
where py >nul 2>nul
if %errorlevel%==0 (
    py -3 python_ai_service\app.py
    exit /b %errorlevel%
)

where python >nul 2>nul
if %errorlevel%==0 (
    python python_ai_service\app.py
    exit /b %errorlevel%
)

echo Python 3 was not found on PATH.
exit /b 1
