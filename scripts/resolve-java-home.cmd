@echo off
setlocal

if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\java.exe" (
        echo %JAVA_HOME%
        exit /b 0
    )
)

for %%D in (
    "C:\Users\KINJEKITILE NGWALE\.jdks\liberica-full-21.0.10"
    "C:\Users\KINJEKITILE NGWALE\.vscode\extensions\redhat.java-1.53.0-win32-x64\jre\21.0.10-win32-x86_64"
    "C:\Users\KINJEKITILE NGWALE\.jdks\corretto-20.0.2.1"
    "C:\Program Files\JetBrains\IntelliJ IDEA 2026.1\jbr"
) do (
    if exist "%%~D\bin\java.exe" (
        echo %%~D
        exit /b 0
    )
)

exit /b 1
