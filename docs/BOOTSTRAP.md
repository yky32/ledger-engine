# Runtime bootstrap (fresh DB)

Empty DB has **no** digestion rules. Ingest policy is created lazily on first read/write with code/env defaults.

**After app is up:**

```bash
./scripts/bootstrap-runtime.sh
# BASE_URL=http://localhost:8080 ./scripts/bootstrap-runtime.sh
```

Idempotent. Creates / refreshes:

| Resource | Default |
|----------|---------|
| `PUT /ingest-policy` | enabled + auto-wallet HKD/LP |
| `PURCHASE_DEFAULT` | EARN `RATE:0.01` · HKD,USD · maxAge 7d |
| `SIGNUP_DEFAULT` | EARN `FIXED:100` |
| `REDEEM_DEFAULT` | BURN `AMOUNT` |

Then:

```bash
./scripts/e2e-smoke.sh
SKIP_ONBOARD=1 ./scripts/e2e-smoke.sh   # first earn auto-creates wallet
```

### Local stack reminder

```bash
# Postgres default: localhost:5433 / ledger-engine
mvn spring-boot:run
# or docker compose up --build
```

See [docs/README.md](./README.md) · [CLIENT_EARN_WEBHOOK.md](./CLIENT_EARN_WEBHOOK.md).

### One-command upstream sim

```bash
./scripts/upstream-sim.sh
# CUST=01A12345678 AMOUNT=500 ./scripts/upstream-sim.sh
# ./scripts/upstream-sim.sh --no-bootstrap bad-jpy
```

### Schema / data

Default `JPA_DDL_AUTO=create` — **each app boot recreates schema** (no durable local data).  
Fine pre-UAT. Later: `JPA_DDL_AUTO=update` or proper migrations.

If you still hit old-volume NOT NULL errors under `update`:

```bash
./scripts/reset-local-db.sh
mvn spring-boot:run
./scripts/upstream-sim.sh
```
