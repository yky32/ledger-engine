# Runtime bootstrap (fresh DB)

Empty DB has **no** digestion rules and only lazy ingest-policy defaults.
Run after app is up:

```bash
./scripts/bootstrap-runtime.sh
# BASE_URL=http://localhost:8080 ./scripts/bootstrap-runtime.sh
```

Creates / refreshes:

| Resource | Default |
|----------|---------|
| `PUT /ingest-policy` | enabled + auto-wallet HKD/LP |
| `PURCHASE_DEFAULT` | EARN `RATE:0.01` · HKD,USD · maxAge 7d |
| `SIGNUP_DEFAULT` | EARN `FIXED:100` |
| `REDEEM_DEFAULT` | BURN `AMOUNT` |

Idempotent. Then:

```bash
./scripts/e2e-smoke.sh
SKIP_ONBOARD=1 ./scripts/e2e-smoke.sh
```
