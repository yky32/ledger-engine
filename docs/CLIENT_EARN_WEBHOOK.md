# Client earn webhook — eligibility → equation → LP credit

**Audience:** integrator playing **upstream** (POS / order service)  
**Depends on:** runtime config ([BOOTSTRAP.md](./BOOTSTRAP.md)) + optional explicit onboard ([CLIENT_WALLET_ONBOARDING.md](./CLIENT_WALLET_ONBOARDING.md))

See also: [INGEST_VS_DIGESTION.md](./INGEST_VS_DIGESTION.md) · [DOUBLE_ENTRY_EARN.md](./DOUBLE_ENTRY_EARN.md)

---

## Flow

```text
POST /integrations/webhooks/transactions
  eventId + associatedIdentifier + eventType + amount + currency + occurredAt
        │
        ▼
  IngestPolicy (door)          ← /ingest-policy
  • isEnabled?
  • isAutoCreateWallet if no wallet
        │
        ▼
  DigestionRule (brain)        ← /digestion-rules
  • match eventType + priority
  • amount / currency / age gates
  • formula → points
        │ fail → failed_transaction_ingest + status SKIPPED
        ▼
  PROGRAM double-entry earn/burn on pointCurrency (default LP)
        │
        ▼
  EARNED / BURNED + legs[]  (idempotent on eventId / movementKey)
```

---

## 0) One-time bootstrap (empty DB)

```bash
./scripts/bootstrap-runtime.sh
# sets ingest-policy + PURCHASE/SIGNUP/REDEEM digestion defaults
```

Or see [DIGESTION_RULES.md](./DIGESTION_RULES.md) / [INGEST_POLICY.md](./INGEST_POLICY.md).

---

## 1) Play upstream — webhook payload

```bash
BASE=http://localhost:8080
CUST=01A12345678
EVENT_ID="txn-$(date +%s)-$RANDOM"

curl -sS -X POST "$BASE/integrations/webhooks/transactions" \
  -H 'Content-Type: application/json' \
  -d "{
    \"eventId\": \"$EVENT_ID\",
    \"associatedIdentifier\": \"$CUST\",
    \"eventType\": \"PURCHASE\",
    \"amount\": 200.00,
    \"currency\": \"HKD\",
    \"occurredAt\": \"$(date -u +%Y-%m-%dT%H:%M:%SZ)\",
    \"metadata\": { \"source\": \"pos-sim\" }
  }" | jq .
```

| Field | Required | Notes |
|-------|----------|--------|
| `eventId` | ✅ | Idempotency key |
| `associatedIdentifier` | ✅ | Same CUST_ID as wallet (`userId` alias accepted) |
| `eventType` | ✅ | Must match a digestion rule (`PURCHASE`, `SIGNUP`, `REDEEM`, …) |
| `amount` | ✅ | Spend formulas need **> 0** |
| `currency` | ✅ | Must be in rule `eligibleCurrencies` when set |
| `occurredAt` | ✅ if maxAgeDays set | ISO-8601 |
| `metadata` | optional | Free map |

Default PURCHASE rule after bootstrap: **EARN** · `RATE:0.01` · HKD/USD · maxAge 7d · LP.

---

## 2) Success response

```json
{
  "code": "SYS0000",
  "data": {
    "eventId": "txn-…",
    "status": "EARNED",
    "operation": "EARN",
    "points": 2.0,
    "walletExternalReference": "01A12345678",
    "movementId": 123,
    "legs": [
      { "entryId": 1, "accountId": 99, "direction": "DEBIT",  "amount": 2, "currency": "LP" },
      { "entryId": 2, "accountId": 88, "direction": "CREDIT", "amount": 2, "currency": "LP" }
    ]
  }
}
```

```bash
# LP balance
curl -sS "$BASE/wallets/$CUST?currencies=LP" | jq .

# legs by event
curl -sS "$BASE/integrations/ledger-entries?eventId=$EVENT_ID" | jq .
```

---

## 3) Failures → DB then SKIPPED

Engine **inserts** `failed_transaction_ingest`, then returns:

```json
{ "data": { "eventId": "…", "status": "SKIPPED", "reason": "…" } }
```

| failureCode | Meaning |
|-------------|---------|
| `AMOUNT` | amount ≤ 0 for RATE/AMOUNT/MUL_ADD |
| `MIN_AMOUNT` | below rule min |
| `CURRENCY` | ccy not eligible |
| `AGE` | missing/old `occurredAt` |
| `NO_RULE` | no matching digestion rule |
| `NO_WALLET` | no wallet and auto-create off |
| `ERROR` | apply failure |
| `DISABLED` | ingest policy `isEnabled=false` |

---

## 4) End-to-end (manual or auto-wallet)

```bash
# Optional explicit onboard (skip if isAutoCreateWallet=true)
curl -sS -X POST "$BASE/wallets" \
  -H 'Content-Type: application/json' \
  -d "{
    \"associatedIdentifier\": \"$CUST\",
    \"settlementCurrency\": \"HKD\",
    \"accounts\": [{ \"currency\": \"LP\", \"name\": \"Loyalty\", \"refCode\": \"LP\" }]
  }"

# Upstream purchase (section 1)
# …
```

Lazy path:

```bash
SKIP_ONBOARD=1 ./scripts/e2e-smoke.sh
```

---

## 5) Query failed ingest (ops)

Pagination is **1-based** (`page`, `size`) — not `limit`.

```bash
curl -sS "$BASE/integrations/failed-transactions?status=OPEN&page=1&size=20"
curl -sS "$BASE/integrations/failed-transactions?associatedIdentifier=$CUST&status=OPEN"
curl -sS "$BASE/integrations/failed-transactions?failureCode=CURRENCY"
curl -sS "$BASE/integrations/failed-transactions?eventId=$EVENT_ID"
```

---

## 6) Fail replay

```bash
# single
curl -sS -X POST "$BASE/integrations/failed-transactions/{id}/review"
curl -sS -X POST "$BASE/integrations/failed-transactions/{id}/replay"
# → REPLAYED when fixed; still-skipped does NOT insert a second fail row

# bulk (max 50)
curl -sS -X POST "$BASE/integrations/failed-transactions/replay" \
  -H 'Content-Type: application/json' \
  -d '{"ids":[1,2,3]}'
```

---

## 7) Smoke

```bash
./scripts/bootstrap-runtime.sh
./scripts/e2e-smoke.sh
# BASE_URL=http://localhost:8080 ./scripts/e2e-smoke.sh
```

---

## 8) Formulas & runtime rate change

| formula | points |
|---------|--------|
| `RATE:0.01` | amount × 0.01 |
| `MUL_ADD:0.01:5` | amount × 0.01 + 5 |
| `FIXED:100` | 100 |
| `AMOUNT` | amount |

```bash
curl -sS -X PUT "$BASE/digestion-rules/{id}" \
  -H 'Content-Type: application/json' \
  -d '{"formula":"RATE:0.02"}'
# next webhook uses new rate — no restart
```

Full rule API: [DIGESTION_RULES.md](./DIGESTION_RULES.md)

---

## 9) Config (runtime)

**Door / auto-wallet** — DB:

```bash
curl -sS "$BASE/ingest-policy"
curl -sS -X PUT "$BASE/ingest-policy" \
  -H 'Content-Type: application/json' \
  -d '{"isEnabled":true,"isAutoCreateWallet":true,"autoWalletSettlementCurrency":"HKD","autoWalletEnsureCurrency":"LP"}'
```

Env only seeds the first empty `ingest_policy` row.  
**Digestion:** `/digestion-rules` only — no YAML rule catalog.

[INGEST_POLICY.md](./INGEST_POLICY.md) · [BOOTSTRAP.md](./BOOTSTRAP.md)
