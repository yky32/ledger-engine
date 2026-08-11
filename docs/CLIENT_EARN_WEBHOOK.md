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

Default PURCHASE rule (example):

| Setting | Default |
|---------|---------|
| operation | EARN |
| min-amount | 0.01 |
| formula | `RATE:0.01` → LP = amount × 0.01 |
| point-currency | LP |
| max-age-days | **7** (configurable) |
| eligible-currencies | **HKD**, **USD** (configurable list) |

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
curl -sS 'http://localhost:8080/integrations/failed-transactions/by-event/txn-xxx'
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

## Config override (env / YAML)

```yaml
ledger.integration.rules:
  - event-type: PURCHASE
    operation: EARN
    min-amount: 0.01
    point-currency: LP
    formula: RATE:0.01
    max-age-days: 7
    eligible-currencies: [HKD, USD, CNY]
```
