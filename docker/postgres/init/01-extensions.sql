-- Optional init for ledger-engine Postgres (runs only on empty data volume).
-- DB/user are created by POSTGRES_* env; this script is a hook for extensions.

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- failed_transaction_ingest and domain tables are created by Spring JPA ddl-auto
-- (see application.yml JPA_DDL_AUTO / spring.jpa.hibernate.ddl-auto).
