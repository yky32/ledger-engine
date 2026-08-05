# External event integration

External systems deliver **transactional events** via webhook or Kafka. Ledger Engine applies
**rule check → Earn / Burn / Process** only. It does **not** onboard wallets during transaction processing.

```text
┌─────────────────────────────────────────────────────────────────┐
│  PRODUCT SETUP (Ledger Engine owns)                             │
│  • ProgramSetupRunner → COA pool accounts (expense / liability) │
│  • POST /wallets → onboard one user wallet               │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│  RUNTIME — external transactional events                        │
│                                                                 │
│  External system ── webhook / kafka ──▶ TransactionRuleEngine   │
│                                              │                  │
│                                              ▼                  │
│                                   Earn / Burn / Process         │
│                                   (wallet must already exist)   │
└─────────────────────────────────────────────────────────────────┘
```

## Client go-live flow

```text
Step 0  Client purchases / deploys Ledger Engine
Step 1  Program pools created on startup (automatic)
Step 2  Client reads CRM → POST /wallets/batch (Phase 1)
Step 3  Client verifies all customers have wallets
Step 4  Client enables POS / campaign → webhook or Kafka (Phase 2)
```

---

## Two separate concepts

| Phase | Concept | Who triggers | API |
|---|---|---|---|
| **1** | Wallet onboarding | Client CRM / membership | `POST /wallets`, `/wallets/batch` |
| **2** | Transaction processing | POS / e-commerce / campaign | Webhook + Kafka |

If Phase 2 runs before Phase 1 completes → **`SKIPPED`** (`Wallet not onboarded`).

---

## 1. Wallet onboarding (Phase 1)

### Single customer (ongoing signup)

```http
POST /wallets
Content-Type: application/json

{
  "userId": "CUST-10001",
  "currency": "LP",
  "name": "Alice wallet"
}
```

### Bulk import from legacy CRM (go-live)

```http
POST /wallets/batch
Content-Type: application/json

{
  "wallets": [
    { "userId": "CRM-0001", "currency": "LP", "name": "Alice" },
    { "userId": "CRM-0002", "currency": "LP", "name": "Bob" }
  ]
}
```

Idempotent — safe to re-run; existing wallets reported in `alreadyExistingUserIds`.

Creates `ledger_account` with `external_reference = wallet:{userId}:LP`.

On application startup, `ProgramSetupRunner` ensures program pool accounts exist:

- `pool:loyalty-expense:LP` (EXPENSE) — used for **Earn**
- `pool:loyalty-liability:LP` (LIABILITY) — used for **Burn**

---

## 2. Transaction processing (Phase 2)

Start only after Phase 1 wallet onboarding is complete.

### Event payload

```json
{
  "eventId": "evt-7b2c",
  "userId": "CUST-10001",
  "eventType": "PURCHASE",
  "amount": 150.00,
  "currency": "LP",
  "occurredAt": "2026-08-05T00:00:00Z",
  "metadata": { "source": "pos" }
}
```

### Integration points

| Channel | Endpoint / topic |
|---|---|
| **Webhook** | `POST /integrations/webhooks/transactions` |
| **Kafka** | topic `ledger.transaction.events` |

### Processing pipeline

```text
1. Rule check     → match eventType, minAmount, operation
2. Wallet lookup  → must exist (no auto-create)
3. Post journal   → Earn / Burn / Process legs
4. Return result  → EARNED | BURNED | PROCESSED | SKIPPED | DUPLICATE
```

### Rules configuration

```yaml
ledger:
  integration:
    rules:
      - event-type: PURCHASE
        operation: EARN
        min-amount: 10
        point-currency: LP
        formula: AMOUNT
      - event-type: REDEEM
        operation: BURN
        min-amount: 1
        point-currency: LP
        formula: AMOUNT
      - event-type: ADJUST
        operation: PROCESS
        process-type: ADJUST
        point-currency: LP
        formula: FIXED:10
```

| Operation | Posting |
|---|---|
| **EARN** | Debit expense pool, credit user wallet |
| **BURN** | Debit user wallet, credit liability pool |
| **PROCESS** | Rule-specific (ADJUST implemented; HOLD/EXPIRE roadmap) |

Formulas: `AMOUNT`, `FIXED:{n}`, `RATE:{n}`.

---

## Docker simulator

Simulates an **external** system. By default it **onboards wallets first**, then shoots events.

```bash
cp .env.example .env
docker compose --profile simulator up --build
```

| Variable | Default | Description |
|---|---|---|
| `SIM_MODE` | `webhook` | `webhook`, `kafka`, or `both` |
| `SIM_WEBHOOK_URL` | `http://app:8080/integrations/webhooks/transactions` | Ledger webhook |
| `SIM_LEDGER_BASE_URL` | derived from webhook URL | Base URL for wallet onboarding |
| `SIM_ONBOARD_WALLETS` | `true` | Call `POST /wallets` before sending events |
| `SIM_INTERVAL_SECONDS` | `2` | Delay between events |
| `SIM_TRANSACTION_COUNT` | `20` | Event count (`0` = unlimited) |
| `SIM_CURRENCY` | `LP` | Event currency |
| `SIM_AMOUNT_MIN` / `MAX` | `10` / `500` | Random amount range |
| `SIM_EVENT_TYPE` | `PURCHASE` | Matched against rules |
| `SIM_USER_COUNT` | `5` | Users to onboard and rotate |

Set `SIM_ONBOARD_WALLETS=false` to simulate external events for users **without** wallets (expect `SKIPPED`).
