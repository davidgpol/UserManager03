#!/usr/bin/env bash
set -euo pipefail
# Only project-generated hexadecimal test passwords enter this bootstrap SQL.
app_password="$(cat /run/secrets/spring.datasource.password)"
[[ "$app_password" =~ ^[0-9a-f]{64}$ ]]
printf "CREATE ROLE um03_app LOGIN PASSWORD '%s';\n" "$app_password" | psql --set ON_ERROR_STOP=1 --username um03_migrate --dbname usermanager03
psql --set ON_ERROR_STOP=1 --username um03_migrate --dbname usermanager03 <<'SQL'
REVOKE CREATE ON SCHEMA public FROM PUBLIC;
GRANT USAGE ON SCHEMA public TO um03_app;
ALTER DEFAULT PRIVILEGES FOR ROLE um03_migrate IN SCHEMA public GRANT SELECT, INSERT, UPDATE ON TABLES TO um03_app;
SQL
unset app_password
