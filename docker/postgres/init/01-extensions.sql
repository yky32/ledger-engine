-- Optional init for ledger-engine Postgres (runs only on empty data volume).
-- DB/user are created by POSTGRES_* env; this script is a hook for extensions.

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- All tables are created by Liquibase at app startup
-- (src/main/resources/db/changelog/changes). pgcrypto is currently unused by
-- the app; kept as a hook.
