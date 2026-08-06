# Simulator — Phase 1 backfill & Phase 2 events

Simulates **UAfinance (or any CRM)** integrating with Ledger Engine.

## Go-live model

```text
Phase 1  CRM export → 1 customer = 1 wallet   (must finish first)
Phase 2  POS / campaign → PURCHASE / REDEEM events
```

For **UAfinance ~70K members**, Phase 1 creates **70K liability wallets** (`wallet:{userId}:LP`).

API used:

- `POST /wallets/batch` — up to **1000** wallets per request (idempotent)

---

## Phase 1: simulate 70K customer backfill

### Docker (recommended)

```bash
# from ledger-engine root
cp .env.example .env

# edit .env — see values below
docker compose --profile simulator up --build
```

`.env` for 70K backfill only:

```env
SIM_MODE=backfill
SIM_CURRENCY=LP
SIM_USER_ID_PREFIX=UAF-
SIM_USER_COUNT=70000
SIM_BATCH_SIZE=500
SIM_BATCH_PAUSE_SECONDS=0.05
SIM_EXTERNAL_TYPE=uafinance
SIM_ONBOARD_WALLETS=true
SIM_TRANSACTION_COUNT=0
```

Generated ids: `UAF-00001` … `UAF-70000` (1:1 synthetic customers).

### Local Python (ledger already on :8080)

```bash
cd simulator
pip install -r requirements.txt

SIM_MODE=backfill \
SIM_LEDGER_BASE_URL=http://localhost:8080 \
SIM_USER_COUNT=70000 \
SIM_USER_ID_PREFIX=UAF- \
SIM_CURRENCY=LP \
SIM_BATCH_SIZE=500 \
python simulator.py
```

### Real CRM export file

Export UAfinance customer ids (one per line or CSV with `userId` / `customerId` / `id`):

```bash
# customers.csv
# userId
# UAF-100001
# UAF-100002

SIM_MODE=backfill \
SIM_CUSTOMER_FILE=./customers.csv \
SIM_CURRENCY=LP \
SIM_BATCH_SIZE=500 \
python simulator.py
```

JSON array also supported: `["id1","id2"]` or `[{"userId":"..."}]`.

---

## What “1:1” means in the engine

| UAfinance | Ledger Engine |
|---|---|
| Customer id | `Wallet.ownerId` + `Wallet.extIdentifier` |
| Loyalty unit | `currency` (e.g. `LP`) |
| Wallet | `POST /wallets` creates LIABILITY `Account` with `fullNumber=wallet:{userId}:LP` |
| Uniqueness | `(ownerId, currency)` and account `fullNumber` |

Re-run is safe: batch returns `alreadyExists` for ids already onboarded.

---

## Throughput tips (70K)

| Setting | Suggestion |
|---|---|
| `SIM_BATCH_SIZE` | `500` (API max `1000`) |
| DB | Prefer **Postgres** (`docker compose` / `postgres` profile), not H2 file for 70K |
| Heap | Give JVM more RAM if needed: `JAVA_OPTS=-Xmx1g` on app |
| Pause | `SIM_BATCH_PAUSE_SECONDS=0.05` reduces spikes |

Rough order of magnitude: hundreds of wallets/sec depending on machine/DB → **a few minutes** for 70K, not hours.

After backfill, verify:

```bash
# sample lookup
curl -s "http://localhost:8080/wallets/UAF-00001/LP" | jq .
curl -s "http://localhost:8080/dashboards" | jq .
```

---

## Phase 2: events after backfill

Small smoke after backfill:

```env
SIM_MODE=backfill
SIM_USER_COUNT=70000
SIM_SMOKE_EVENTS_AFTER_BACKFILL=20
SIM_SMOKE_MODE=webhook
```

Or full event sim on already-onboarded users:

```env
SIM_MODE=webhook
SIM_ONBOARD_WALLETS=false
SIM_USER_COUNT=70000
SIM_USER_ID_PREFIX=UAF-
SIM_TRANSACTION_COUNT=100
SIM_INTERVAL_SECONDS=0.2
```

Missing wallets → ingestion `SKIPPED` (`Wallet not onboarded`).

---

## Env reference

| Variable | Default | Meaning |
|---|---|---|
| `SIM_MODE` | `webhook` | `backfill` \| `webhook` \| `kafka` \| `both` |
| `SIM_USER_COUNT` | `5` | Synthetic customers to generate |
| `SIM_USER_ID_PREFIX` | `UAF-` | Prefix for synthetic ids |
| `SIM_CUSTOMER_FILE` | _(empty)_ | CSV/JSON/lines of real CRM ids |
| `SIM_CURRENCY` | `LP` | Wallet currency |
| `SIM_BATCH_SIZE` | `500` | Batch size (max 1000) |
| `SIM_BATCH_PAUSE_SECONDS` | `0.05` | Pause between batches |
| `SIM_EXTERNAL_TYPE` | `uafinance` | Stored on wallet |
| `SIM_ONBOARD_WALLETS` | `true` | Pre-onboard for event modes |
| `SIM_SMOKE_EVENTS_AFTER_BACKFILL` | `0` | Events after backfill |
| `SIM_WAIT_FOR_HEALTH` | `true` | Wait for `/actuator/health` |

---

## Production backfill (not the simulator)

1. Export UAfinance customer master → CSV (`userId`, optional name).  
2. Run same batch loop (this script or your ETL) against **staging**, then production.  
3. Confirm counts: CRM rows ≈ `wallet` rows for currency.  
4. Only then enable Phase 2 event traffic.
