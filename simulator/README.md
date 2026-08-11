# Simulator — Phase 1 backfill & Phase 2 events

Generic **CRM / integrator** simulator for Ledger Engine.  
Client-specific product catalogs (account lines, card codes, …) are **not** modeled here — fill those via SDK / real integrations.

## Go-live model

```text
Phase 1  CRM export → 1 customer = 1 wallet   (must finish first)
Phase 2  POS / campaign → PURCHASE / REDEEM events
```

Phase 1 creates one liability wallet per customer id (`wallet:{associatedIdentifier}:{currency}`).

API used:

- `POST /wallets/batch` — up to **1000** wallets per request (soft-idempotent)

---

## Phase 1: bulk customer backfill

### Docker (recommended)

```bash
# from ledger-engine root
cp .env.example .env
docker compose --profile simulator up --build
```

`.env` for a large synthetic backfill:

```env
SIM_MODE=backfill
SIM_CURRENCY=LP
SIM_USER_ID_PREFIX=CUST-
SIM_USER_COUNT=10000
SIM_BATCH_SIZE=500
SIM_BATCH_PAUSE_SECONDS=0.05
SIM_ASSOCIATED_FROM=CRM
SIM_ONBOARD_WALLETS=true
SIM_TRANSACTION_COUNT=0
```

Generated ids: `CUST-00001` … `CUST-10000` (1:1 synthetic customers).

### Local Python (ledger already on :8080)

```bash
cd simulator
pip install -r requirements.txt

SIM_MODE=backfill \
SIM_LEDGER_BASE_URL=http://localhost:8080 \
SIM_USER_COUNT=1000 \
SIM_USER_ID_PREFIX=CUST- \
SIM_CURRENCY=LP \
SIM_BATCH_SIZE=500 \
python simulator.py
```

### Real CRM export file

Export customer ids (one per line or CSV with `associatedIdentifier` / `userId` / `customerId` / `id`):

```bash
# customers.csv
# associatedIdentifier
# CUST-100001
# CUST-100002

SIM_MODE=backfill \
SIM_CUSTOMER_FILE=./customers.csv \
SIM_CURRENCY=LP \
SIM_BATCH_SIZE=500 \
python simulator.py
```

JSON array also supported: `["id1","id2"]` or `[{"associatedIdentifier":"..."}]`.

---

## What “1:1” means in the engine

| CRM / client | Ledger Engine |
|---|---|
| Customer id | `Wallet.ownerId` + `Wallet.associatedIdentifier` |
| Default settlement currency | `Wallet.settlementCurrency` (+ primary account currency at onboard) |
| Wallet | **1 customer → 1 wallet** (`uk_wallet_owner` on `owner_id`) |
| Account | One primary COA account opened in default currency |
| Uniqueness | `owner_id` (not per-currency wallets) |

Re-run is safe: batch returns `alreadyExists` for ids already onboarded.

---

## Throughput tips (large backfill)

| Setting | Suggestion |
|---|---|
| `SIM_BATCH_SIZE` | `500` (API max `1000`) |
| DB | Prefer **Postgres** (`docker compose`) for large volumes |
| Heap | Give JVM more RAM if needed: `JAVA_OPTS=-Xmx1g` on app |
| Pause | `SIM_BATCH_PAUSE_SECONDS=0.05` reduces spikes |

After backfill, verify:

```bash
curl -s "http://localhost:8080/wallets/CUST-00001/LP" | jq .
curl -s "http://localhost:8080/dashboards" | jq .
```

---

## Phase 2: events after backfill

Small smoke after backfill:

```env
SIM_MODE=backfill
SIM_USER_COUNT=1000
SIM_SMOKE_EVENTS_AFTER_BACKFILL=20
SIM_SMOKE_MODE=webhook
```

Or full event sim on already-onboarded users:

```env
SIM_MODE=webhook
SIM_ONBOARD_WALLETS=false
SIM_USER_COUNT=1000
SIM_USER_ID_PREFIX=CUST-
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
| `SIM_USER_ID_PREFIX` | `CUST-` | Prefix for synthetic ids |
| `SIM_CUSTOMER_FILE` | _(empty)_ | CSV/JSON/lines of real CRM ids |
| `SIM_CURRENCY` | `LP` | Wallet currency |
| `SIM_BATCH_SIZE` | `500` | Batch size (max 1000) |
| `SIM_BATCH_PAUSE_SECONDS` | `0.05` | Pause between batches |
| `SIM_ASSOCIATED_FROM` | `CRM` | Stored on wallet as `associatedFrom` |
| `SIM_ONBOARD_WALLETS` | `true` | Pre-onboard for event modes |
| `SIM_SMOKE_EVENTS_AFTER_BACKFILL` | `0` | Events after backfill |
| `SIM_WAIT_FOR_HEALTH` | `true` | Wait for `/actuator/health` |

---

## Production backfill (not the simulator)

1. Export customer master → CSV (`associatedIdentifier`, optional name).  
2. Run same batch loop (this script or your ETL) against **staging**, then production.  
3. Confirm counts: CRM rows ≈ `wallet` rows for currency.  
4. Only then enable Phase 2 event traffic.  
5. Multi-line account-sets (`accounts.refCode`) are filled by the **client SDK**, not this simulator.
