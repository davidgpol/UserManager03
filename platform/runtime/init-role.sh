#!/usr/bin/env bash
set -euo pipefail
migrate_password="$(cat /run/secrets/migrate-password)"
app_password="$(cat /run/secrets/app-password)"
[[ "$app_password" =~ ^[0-9a-f]{64}$ && "$migrate_password" =~ ^[0-9a-f]{64}$ ]]
{
  printf "CREATE ROLE um03_migrate LOGIN PASSWORD '%s';\n" "$migrate_password"
  printf "CREATE ROLE um03_app LOGIN PASSWORD '%s';\n" "$app_password"
  cat <<'SQL'
REVOKE ALL ON DATABASE usermanager03 FROM PUBLIC;
GRANT CONNECT ON DATABASE usermanager03 TO um03_migrate, um03_app;
REVOKE CREATE ON SCHEMA public FROM PUBLIC;
ALTER SCHEMA public OWNER TO um03_migrate;
GRANT USAGE ON SCHEMA public TO um03_app;
ALTER DEFAULT PRIVILEGES FOR ROLE um03_migrate IN SCHEMA public GRANT SELECT, INSERT, UPDATE ON TABLES TO um03_app;
SQL
} | psql --set ON_ERROR_STOP=1 --username um03_bootstrap --dbname usermanager03
unset app_password migrate_password
