@echo off
setlocal
set PGPASSWORD=Nsunga@2018
set PGCONNECT_TIMEOUT=5
C:\Progra~1\PostgreSQL\18\bin\pg_isready.exe -h localhost -p 5432 -U postgres
C:\Progra~1\PostgreSQL\18\bin\psql.exe -w -h localhost -p 5432 -U postgres -d postgres -c "select 1;"
C:\Progra~1\PostgreSQL\18\bin\psql.exe -w -h localhost -p 5432 -U postgres -lqt
