# Runtime bootstrap (fresh DB)

Empty DB has **no** digestion rules. Ingest policy is created lazily on first read/write with code/env defaults.

## Recommended local e2e (one command)

Restarts engine with **`JPA_DDL_AUTO=create`**, bootstraps rules, fires a **configurable txn matrix** (good + filter rejects):

```bash
./scripts/upstream-sim.sh
# same:
./scripts/upstream-sim.sh e2e
```

Then review in admin portal: paste printed `CUST=` into http://localhost:3000/review

### Configure counts / amounts

```bash
cp scripts/upstream-sim.env.example scripts/upstream-sim.env
# edit COUNT_* and AMOUNT_MIN/MAX

COUNT_PURCHASE_OK=10 COUNT_PURCHASE_JPY=5 AMOUNT_MIN=20 AMOUNT_MAX=900 \
  ./scripts/upstream-sim.sh
```

| Count env | Default | Expect |
|-----------|---------|--------|
| `COUNT_PURCHASE_OK` | 5 | EARNED HKD |
| `COUNT_PURCHASE_USD` | 2 | EARNED USD |
| `COUNT_PURCHASE_JPY` | 2 | SKIPPED currency |
| `COUNT_TOO_SMALL` | 1 | SKIPPED amount 0 |
| `COUNT_TOO_OLD` | 1 | SKIPPED age (30d) |
| `COUNT_SIGNUP` | 1 | EARNED fixed |
| `COUNT_REDEEM` | 1 | BURN 1 LP |
| `COUNT_DUPLICATE` | 1 | DUPLICATE |

Flags: `--no-restart` · `--no-bootstrap` · `--stop-server`

---

## Manual pieces

```bash
./scripts/bootstrap-runtime.sh
./scripts/e2e-smoke.sh
SKIP_ONBOARD=1 ./scripts/e2e-smoke.sh
./scripts/reset-local-db.sh
```

| Resource | Default after bootstrap |
|----------|-------------------------|
| `PUT /ingest-policy` | enabled + auto-wallet HKD/LP |
| `PURCHASE_DEFAULT` | EARN `RATE:0.01` · HKD,USD · maxAge 7d |
| `SIGNUP_DEFAULT` | EARN `FIXED:100` |
| `REDEEM_DEFAULT` | BURN `AMOUNT` |

### Schema

Default `JPA_DDL_AUTO=create` — each boot recreates schema (no durable local data).  
`upstream-sim.sh e2e` always starts with `create`.
