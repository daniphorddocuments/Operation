@echo off
setlocal
cd /d "%~dp0.."

for /f "usebackq delims=" %%I in (`call scripts\resolve-java-home.cmd`) do set "JAVA_HOME=%%I"

if not defined JAVA_HOME (
    echo Compatible Java runtime not found. Set JAVA_HOME to JDK 21 or JDK 20.
    exit /b 1
)

set "Path=%JAVA_HOME%\bin;%Path%"

if not exist ".\mvnw.cmd" (
    echo Maven wrapper not found.
    exit /b 1
)

call ".\mvnw.cmd" -q -DskipTests compile
