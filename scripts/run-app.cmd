@echo off
setlocal
setlocal EnableDelayedExpansion
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

if not defined SERVER_PORT if not defined PORT (
    for /f "usebackq delims=" %%P in (`powershell -NoProfile -Command "$ports = 8080..8099; foreach ($port in $ports) { $listener = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue; if (-not $listener) { $port; break } }"`) do set "SERVER_PORT=%%P"
    if not defined SERVER_PORT set "SERVER_PORT=8080"
)

if not defined SPRING_DATASOURCE_URL (
    for /f "usebackq delims=" %%D in (`powershell -NoProfile -Command "$dataDir = Join-Path (Get-Location) 'data'; if (-not (Test-Path $dataDir)) { New-Item -ItemType Directory -Path $dataDir | Out-Null }; $baseName = 'froms-runtime-' + $env:SERVER_PORT; $selected = $null; for ($index = 0; $index -lt 100; $index++) { $candidate = if ($index -eq 0) { $baseName } else { '{0}-{1}' -f $baseName, $index }; $lockFile = Join-Path $dataDir ($candidate + '.lock.db'); if (-not (Test-Path $lockFile)) { $selected = $candidate; break } }; if (-not $selected) { $selected = '{0}-{1}' -f $baseName, [DateTimeOffset]::UtcNow.ToUnixTimeSeconds() }; $selected"`) do set "FROMS_RUNTIME_DB_NAME=%%D"
    if not defined FROMS_RUNTIME_DB_NAME set "FROMS_RUNTIME_DB_NAME=froms-runtime-!SERVER_PORT!"
    set "SPRING_DATASOURCE_URL=jdbc:h2:file:./data/!FROMS_RUNTIME_DB_NAME!;MODE=PostgreSQL;AUTO_SERVER=TRUE;AUTO_SERVER_PORT=0"
)

echo Starting FROMS on port !SERVER_PORT!
echo Using datasource !SPRING_DATASOURCE_URL!

if /I "%FROMS_USE_H2%"=="true" (
    if not defined SPRING_DATASOURCE_URL (
        set "SPRING_DATASOURCE_URL=jdbc:h2:file:./data/froms;MODE=PostgreSQL;AUTO_SERVER=TRUE;AUTO_SERVER_PORT=0"
    )

    if not defined SPRING_DATASOURCE_USERNAME (
        set "SPRING_DATASOURCE_USERNAME=sa"
    )

    if not defined SPRING_DATASOURCE_PASSWORD (
        set "SPRING_DATASOURCE_PASSWORD="
    )
)

call ".\mvnw.cmd" spring-boot:run
