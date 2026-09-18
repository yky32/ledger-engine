#!/usr/bin/env bash
# Reset local app DB (ledger-engine only). Keeps ledger-engine-test.
#
#   ./scripts/reset-local-db.sh
#   POSTGRES_CONTAINER=ledger-engine-postgres ./scripts/reset-local-db.sh
#
# Why: Liquibase owns schema now. Reset is only needed to clear data, or to
# recover a dirty pre-Liquibase volume (the changelog precondition HALTs on
# tables that exist without a DATABASECHANGELOG entry).
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

# Ensure the test DB exists (nothing else provisions it; tests: mvn test →
# Liquibase drop-first rebuilds its schema every run).
TEST_DB="${POSTGRES_TEST_DB:-ledger-engine-test}"
if ! docker exec "$CONTAINER" psql -U "$USER" -tAc \
  "SELECT 1 FROM pg_database WHERE datname = '$TEST_DB';" | grep -q 1; then
  info "CREATE $TEST_DB (missing)"
  docker exec "$CONTAINER" psql -U "$USER" -c "CREATE DATABASE \"$TEST_DB\" OWNER $USER;"
fi

green "OK — empty $DB. Restart app: mvn spring-boot:run"
green "Then: ./scripts/upstream-sim.sh"
