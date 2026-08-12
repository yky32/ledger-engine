#!/usr/bin/env bash
# Reset local app DB (ledger-engine only). Keeps ledger-engine-test.
#
#   ./scripts/reset-local-db.sh
#   POSTGRES_CONTAINER=ledger-engine-postgres ./scripts/reset-local-db.sh
#
# Why: Hibernate ddl-auto=update cannot ADD NOT NULL columns when old rows exist
# (e.g. settlement_currency). Greenfield local is the supported path.
set -euo pipefail

CONTAINER="${POSTGRES_CONTAINER:-ledger-engine-postgres}"
DB="${POSTGRES_DB:-ledger-engine}"
USER="${POSTGRES_USER:-postgres}"

red() { printf '\033[31m%s\033[0m\n' "$*"; }
green() { printf '\033[32m%s\033[0m\n' "$*"; }
info() { printf '→ %s\n' "$*"; }

command -v docker >/dev/null || { red "docker required"; exit 1; }
docker exec "$CONTAINER" pg_isready -U "$USER" >/dev/null \
  || { red "container $CONTAINER not ready"; exit 1; }

info "terminate connections on $DB"
docker exec "$CONTAINER" psql -U "$USER" -c \
  "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '$DB' AND pid <> pg_backend_pid();" \
  >/dev/null

info "DROP + CREATE $DB"
docker exec "$CONTAINER" psql -U "$USER" -c "DROP DATABASE IF EXISTS \"$DB\";"
docker exec "$CONTAINER" psql -U "$USER" -c "CREATE DATABASE \"$DB\" OWNER $USER;"

green "OK — empty $DB. Restart app: mvn spring-boot:run"
green "Then: ./scripts/upstream-sim.sh"
