# Client earn webhook — eligibility → equation → LP credit

**Audience:** integrator  
**Depends on:** wallet onboard with **LP account** (see [CLIENT_WALLET_ONBOARDING.md](./CLIENT_WALLET_ONBOARDING.md))

---

## Flow

```text
POST /integrations/webhooks/transactions
  associatedIdentifier + amount + currency + occurredAt + eventType
        │
        ▼
  Gates (config per rule)
  • amount > 0 (spend formulas RATE/AMOUNT)
  • amount >= minAmount
  • currency ∈ eligibleCurrencies
  • occurredAt within maxAgeDays (required if maxAgeDays set)
        │ fail → row in failed_transaction_ingest + SKIPPED response
        ▼
  points = formula(amount)   e.g. RATE:0.01 → amount * 0.01
        │
        ▼
  wallet[associatedIdentifier] → account[LP] += points
        │
        ▼
  EARNED (idempotent on eventId)
```

Burn is out of scope for this guide.

---

## 1) Webhook payload

```bash
curl -sS -X POST 'http://localhost:8080/integrations/webhooks/transactions' \
  -H 'Content-Type: application/json' \
  -d '{
    "eventId": "txn-20260811-0001",
    "associatedIdentifier": "01A12345678",
    "eventType": "PURCHASE",
    "amount": 200.00,
    "currency": "HKD",
    "occurredAt": "2026-08-11T10:00:00Z",
    "metadata": { "source": "pos" }
  }'
```

| Field | Required | Notes |
|-------|----------|--------|
| `eventId` | ✅ | Idempotency key |
| `associatedIdentifier` | ✅ | Same CUST_ID as wallet create (`userId` alias accepted) |
| `eventType` | ✅ | Must match a rule (default `PURCHASE`) |
| `amount` | ✅ | Txn amount; spend formulas need **> 0** |
| `currency` | ✅ | Must be in rule `eligibleCurrencies` when configured |
| `occurredAt` | ✅ if maxAgeDays set | ISO-8601; older than N days → skip + **fail table** |
| `metadata` | optional | Free map |

Default PURCHASE digestion rule example (create via API — not YAML):

| Setting | Example |
|---------|---------|
| operation | EARN |
| min-amount | 0.01 |
| formula | `RATE:0.01` → LP = amount × 0.01 |
| point-currency | LP |
| max-age-days | **7** |
| eligible-currencies | **HKD**, **USD** |

See [DIGESTION_RULES.md](./DIGESTION_RULES.md).

---

## 2) Success response

```json
{
  "code": "SYS0000",
  "data": {
    "eventId": "txn-20260811-0001",
    "status": "EARNED",
    "operation": "EARN",
    "points": 2.0,
    "walletExternalReference": "01A12345678"
  }
}
```

Verify LP balance:

```bash
curl -sS 'http://localhost:8080/wallets/01A12345678?currencies=LP'
```

---

## 3) Failures → DB then SKIPPED

On eligibility / no-wallet / apply error the engine **first inserts** `failed_transaction_ingest`, then returns:

```json
{ "data": { "eventId": "...", "status": "SKIPPED", "reason": "..." } }
```

| failureCode | Meaning |
|-------------|---------|
| `AMOUNT` | amount ≤ 0 for RATE/AMOUNT formula |
| `MIN_AMOUNT` | below rule min |
| `CURRENCY` | ccy not in eligible list |
| `AGE` | missing `occurredAt` or older than maxAgeDays |
| `NO_RULE` | no eventType match |
| `NO_WALLET` | customer not onboarded |
| `ERROR` | apply/balance failure |
| `DISABLED` | integration off |

Table (JPA): `failed_transaction_ingest`  
Columns include: `event_id`, `associated_identifier`, `failure_code`, `reason`, `raw_payload`, `status=OPEN`.

---

## 4) End-to-end

```bash
# A) Onboard HKD settlement + LP book
curl -sS -X POST 'http://localhost:8080/wallets' \
  -H 'Content-Type: application/json' \
  -d '{
    "associatedIdentifier": "01A12345678",
    "settlementCurrency": "HKD",
    "accounts": [{ "currency": "LP", "name": "Loyalty", "refCode": "LP" }]
  }'

# B) Purchase webhook
curl -sS -X POST 'http://localhost:8080/integrations/webhooks/transactions' \
  -H 'Content-Type: application/json' \
  -d '{
    "eventId": "txn-e2e-1",
    "associatedIdentifier": "01A12345678",
    "eventType": "PURCHASE",
    "amount": 200,
    "currency": "HKD",
    "occurredAt": "'$(date -u +%Y-%m-%dT%H:%M:%SZ)'"
  }'

# C) Query LP
curl -sS 'http://localhost:8080/wallets/01A12345678?currencies=LP'
```

---

## 5) Query failed ingest (ops)

```bash
# list OPEN skips for a customer
curl -sS 'http://localhost:8080/integrations/failed-transactions?associatedIdentifier=01A12345678&status=OPEN&limit=20'

# by failure code
curl -sS 'http://localhost:8080/integrations/failed-transactions?failureCode=CURRENCY&limit=50'

# by event id
curl -sS 'http://localhost:8080/integrations/failed-transactions?eventId=txn-xxx'
```

---

## 6) E2E smoke script

With app + Postgres running:

```bash
chmod +x scripts/e2e-smoke.sh
./scripts/e2e-smoke.sh
# BASE_URL=http://localhost:8080 ./scripts/e2e-smoke.sh
```

---

## Auto wallet on webhook

When `ledger.integration.auto-create-wallet=true` (default):

```text
gates pass → wallet missing?
  → create settlement HKD + LP book
  → earn/burn same request (same TX)
```

No separate onboard required for first eligible event.

```bash
# lazy provision smoke
SKIP_ONBOARD=1 ./scripts/e2e-smoke.sh
```

Disable: `LEDGER_AUTO_CREATE_WALLET=false` → missing wallet becomes `NO_WALLET` fail row again.

---

Formulas (runtime `digestion_rule.formula`):

| formula | points |
|---------|--------|
| `RATE:0.01` | amount × 0.01 |
| `MUL_ADD:0.01:5` | amount × 0.01 + 5 |
| `FIXED:100` | 100 |
| `AMOUNT` | amount |

**Change rate without restart:**

```bash
curl -sS -X PUT 'http://localhost:8080/digestion-rules/{id}' \
  -H 'Content-Type: application/json' \
  -d '{"formula":"RATE:0.02"}'
```

Full API: [DIGESTION_RULES.md](./DIGESTION_RULES.md)

---

## Config (runtime)

**Door / auto-wallet** — DB, not rule YAML:

```bash
curl -sS 'http://localhost:8080/ingest-policy'
curl -sS -X PUT 'http://localhost:8080/ingest-policy' \
  -H 'Content-Type: application/json' \
  -d '{"isEnabled":true,"isAutoCreateWallet":true,"autoWalletSettlementCurrency":"HKD","autoWalletEnsureCurrency":"LP"}'
```

Env values (if present) only seed the first `ingest_policy` row when empty.  
**Digestion rules:** only via `/digestion-rules` (DB) — no YAML catalog.

See [INGEST_POLICY.md](./INGEST_POLICY.md) · [DIGESTION_RULES.md](./DIGESTION_RULES.md) · [BOOTSTRAP.md](./BOOTSTRAP.md)

---

## Fail replay (ops)

```bash
# list OPEN
curl -sS 'http://localhost:8080/integrations/failed-transactions?status=OPEN&limit=20'

# mark reviewed (no re-run)
curl -sS -X POST "http://localhost:8080/integrations/failed-transactions/{id}/review"

# replay payload through webhook pipeline
curl -sS -X POST "http://localhost:8080/integrations/failed-transactions/{id}/replay"
# → data.status REPLAYED + data.ingestion (EARNED/…) when fixed
```
