# External event integration

External systems deliver **transactional events** via webhook (Kafka optional). Ledger Engine runs:

```text
IngestPolicy (door) → DigestionRule (brain) → wallet + PROGRAM double-entry
```

Detail playbooks: **[docs/START_HERE.md](docs/START_HERE.md)** ⭐ · [docs/CLIENT_EARN_WEBHOOK.md](docs/CLIENT_EARN_WEBHOOK.md) · [docs/INGEST_VS_DIGESTION.md](docs/INGEST_VS_DIGESTION.md)

## Java client SDK

Product backends may use **ledger-engine-sdk** (Java 17) instead of hand-written HTTP.

| Topic | Where |
|-------|--------|
| SDK overview & channels | [ledger-engine-sdk docs/OVERVIEW.md](https://github.com/yky32/ledger-engine-sdk/blob/main/docs/OVERVIEW.md) |
| JAR delivery | [docs/DELIVERY.md](https://github.com/yky32/ledger-engine-sdk/blob/main/docs/DELIVERY.md) |
| Client install | [docs/INTEGRATION.md](https://github.com/yky32/ledger-engine-sdk/blob/main/docs/INTEGRATION.md) |

SDK is **not** on Maven Central (manual versioned JAR).

---

```text
┌─────────────────────────────────────────────────────────────────┐
│  PRODUCT SETUP                                                  │
│  • ./scripts/bootstrap-runtime.sh  → ingest-policy + rules      │
│  • POST /wallets (optional if auto-wallet on)                   │
│  • PROGRAM pool auto-bootstraps on first earn/burn              │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│  RUNTIME — external transactional events                        │
│                                                                 │
│  Upstream ── POST /integrations/webhooks/transactions ──▶       │
│       IngestPolicy → DigestionRule → EARN/BURN + legs           │
└─────────────────────────────────────────────────────────────────┘
```

## Client go-live flow

```text
Step 0  Deploy Ledger Engine + Postgres
Step 1  bootstrap-runtime.sh (ingest-policy + digestion defaults)
Step 2  Optional: CRM batch POST /wallets/batch
Step 3  Enable POS / order system → webhook
```

---

## Two concepts

| Phase | Concept | Who | API |
|---|---|---|---|
| **Setup** | Runtime policy + rules | Ops | `/ingest-policies`, `/digestion-rules`, bootstrap script |
| **1** | Wallet onboarding | CRM / membership | `POST /wallets`, `/wallets/batch` (or auto-wallet) |
| **2** | Transaction processing | POS / e-com | `POST /integrations/webhooks/transactions` |

If Phase 2 runs without wallet and **auto-wallet off** → `SKIPPED` / `NO_WALLET`.  
If **auto-wallet on** (default after bootstrap) → first eligible event creates HKD+LP wallet in same TX.

---

## 1. Wallet onboarding

See [docs/CLIENT_WALLET_ONBOARDING.md](docs/CLIENT_WALLET_ONBOARDING.md).

```http
POST /wallets
Content-Type: application/json

{
  "associatedIdentifier": "01A12345678",
  "settlementCurrency": "HKD",
  "name": "Alice wallet",
  "associatedFrom": "CRM",
  "accounts": [{ "currency": "LP", "name": "Loyalty", "refCode": "LP" }]
}
```

Customer key: `associatedIdentifier` only. Bulk: `POST /wallets/batch` (idempotent).

PROGRAM pool is **lazy** on first earn/burn (not startup YAML seed).

---

## 2. Transaction processing

### Event payload

```json
{
  "eventId": "evt-7b2c",
  "associatedIdentifier": "01A12345678",
  "eventType": "PURCHASE",
  "amount": 150.00,
  "currency": "HKD",
  "occurredAt": "2026-08-12T00:00:00Z",
  "metadata": { "source": "pos" }
}
```

(`userId` is accepted as alias for `associatedIdentifier`.)

### Channel

| Channel | Endpoint |
|---|---|
| **Webhook** | `POST /integrations/webhooks/transactions` |

### Pipeline

```text
1. IngestPolicy     → enabled? auto-wallet?
2. DigestionRule    → match eventType, gates, formula → points
3. Wallet resolve   → create if policy allows
4. Double-entry     → PROGRAM pool + customer LP (movementKey = loyalty-{op}-{eventId})
5. Result           → EARNED | BURNED | PROCESSED | SKIPPED | DUPLICATE + legs[]
```

### Rules configuration (DB — not YAML)

```bash
./scripts/bootstrap-runtime.sh
# or POST/PUT /digestion-rules  — see docs/DIGESTION_RULES.md
```

| Operation | Posting |
|---|---|
| **EARN** | DEBIT PROGRAM.LP · CREDIT customer.LP |
| **BURN** | DEBIT customer.LP · CREDIT PROGRAM.LP |

Query legs: `GET /integrations/ledger-entries?eventId=` or `?movementId=`  
Fail ops: `GET/POST /integrations/failed-transactions…` — [docs/HISTORY_ASOF_REPLAY.md](docs/HISTORY_ASOF_REPLAY.md)

Full curls: [docs/CLIENT_EARN_WEBHOOK.md](docs/CLIENT_EARN_WEBHOOK.md) · [docs/DOUBLE_ENTRY_EARN.md](docs/DOUBLE_ENTRY_EARN.md)

---

## Simulator (optional)

```bash
cp .env.example .env
docker compose --profile simulator up --build
```

| Env | Default | |
|-----|---------|--|
| `SIM_MODE` | `webhook` | `webhook` / `kafka` / `both` |
| `SIM_WEBHOOK_URL` | `http://app:8080/integrations/webhooks/transactions` | |
| `SIM_LEDGER_BASE_URL` | derived | wallet onboard base |

---

## Related docs

Index: **[docs/START_HERE.md](docs/START_HERE.md)**
