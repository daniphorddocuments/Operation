@echo off
setlocal
set PGPASSWORD=Nsunga@2018
set PGCONNECT_TIMEOUT=5
echo ==== TABLES ====
C:\Progra~1\PostgreSQL\18\bin\psql.exe -w -h localhost -p 5432 -U postgres -d shop -c "\dt"
echo ==== USER TABLE ====
C:\Progra~1\PostgreSQL\18\bin\psql.exe -w -h localhost -p 5432 -U postgres -d shop -c "\d user"
echo ==== USERS TABLE ====
C:\Progra~1\PostgreSQL\18\bin\psql.exe -w -h localhost -p 5432 -U postgres -d shop -c "\d users"
echo ==== SHOP TABLE ====
C:\Progra~1\PostgreSQL\18\bin\psql.exe -w -h localhost -p 5432 -U postgres -d shop -c "\d shop"
echo ==== PRODUCT TABLE ====
C:\Progra~1\PostgreSQL\18\bin\psql.exe -w -h localhost -p 5432 -U postgres -d shop -c "\d product"
echo ==== SALE TABLE ====
C:\Progra~1\PostgreSQL\18\bin\psql.exe -w -h localhost -p 5432 -U postgres -d shop -c "\d sale"
