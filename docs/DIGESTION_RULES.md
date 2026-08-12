# Digestion rules — runtime earn filter + formula

**No restart.** Rules live in DB table `digestion_rule`, API `/digestion-rules`.  
YAML `ledger.integration.rules` **seeds** the table only when empty.

---

## Flow

```text
YAML (first boot seed)
        │
        ▼
digestion_rule (DB)  ←── CRUD / enable / disable via API
        │
        ▼
TransactionRuleEngine.evaluate(event)
        │
        ▼
webhook earn / burn (unchanged ingest path)
```

---

## API

```bash
# list
curl -sS 'http://localhost:8080/digestion-rules'
curl -sS 'http://localhost:8080/digestion-rules?enabledOnly=true'

# get
curl -sS 'http://localhost:8080/digestion-rules/1'
curl -sS 'http://localhost:8080/digestion-rules/by-code/PURCHASE_SEED_1'

# create
curl -sS -X POST 'http://localhost:8080/digestion-rules' \
  -H 'Content-Type: application/json' \
  -d '{
    "code": "PURCHASE_VIP",
    "name": "VIP 2%",
    "eventType": "PURCHASE",
    "operation": "EARN",
    "isEnabled": true,
    "priority": 1,
    "minAmount": 0.01,
    "eligibleCurrencies": ["HKD","USD"],
    "maxAgeDays": 7,
    "pointCurrency": "LP",
    "formula": "RATE:0.02"
  }'

# update formula (next webhook uses it immediately)
curl -sS -X PUT 'http://localhost:8080/digestion-rules/1' \
  -H 'Content-Type: application/json' \
  -d '{"formula":"MUL_ADD:0.01:5"}'

# enable / disable
curl -sS -X POST 'http://localhost:8080/digestion-rules/1/disable'
curl -sS -X POST 'http://localhost:8080/digestion-rules/1/enable'
```

**Priority:** lower number wins first among matching `eventType`.

---

## Formula

| formula | meaning |
|---------|---------|
| `AMOUNT` | points = amount |
| `RATE:0.01` | amount × 0.01 |
| `FIXED:100` | constant 100 |
| `MUL_ADD:0.01:5` | amount × 0.01 **+** 5 |
| `{"rate":0.01,"fixed":0}` | same as MUL_ADD (JSON) |

---

## Webhook earn (unchanged shape)

```bash
curl -sS -X POST 'http://localhost:8080/integrations/webhooks/transactions' \
  -H 'Content-Type: application/json' \
  -d '{
    "eventId": "txn-1",
    "associatedIdentifier": "01A12345678",
    "eventType": "PURCHASE",
    "amount": 200,
    "currency": "HKD",
    "occurredAt": "2026-08-12T00:00:00Z"
  }'
```

See also [CLIENT_EARN_WEBHOOK.md](./CLIENT_EARN_WEBHOOK.md).

---

## Phase note

- **P1 done:** runtime digestion-rules + formula  
- **P3 later:** true double-entry legs visibility (task §D)
