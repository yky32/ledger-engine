# Digestion rules — runtime earn filter + formula

**No restart. No YAML seed.**  
Rules live only in DB (`digestion_rule`), managed via **`/digestion-rules`**.

Empty table ⇒ webhook matches nothing (`NO_RULE`) until you create rules via API.

---

## Flow

```text
POST/PUT /digestion-rules  →  digestion_rule (DB)
                                    │
                                    ▼
                    TransactionRuleEngine.evaluate(event)
                                    │
                                    ▼
                         webhook earn / burn
```

---

## Bootstrap (ops)

```bash
# create PURCHASE earn rule
curl -sS -X POST 'http://localhost:8080/digestion-rules' \
  -H 'Content-Type: application/json' \
  -d '{
    "code": "PURCHASE_DEFAULT",
    "name": "Purchase 1%",
    "eventType": "PURCHASE",
    "operation": "EARN",
    "isEnabled": true,
    "priority": 10,
    "minAmount": 0.01,
    "eligibleCurrencies": ["HKD","USD"],
    "maxAgeDays": 7,
    "pointCurrency": "LP",
    "formula": "RATE:0.01"
  }'
```

---

## API

```bash
curl -sS 'http://localhost:8080/digestion-rules'
curl -sS 'http://localhost:8080/digestion-rules?enabledOnly=true'
curl -sS 'http://localhost:8080/digestion-rules/{id}'
curl -sS 'http://localhost:8080/digestion-rules/by-code/PURCHASE_DEFAULT'

curl -sS -X PUT 'http://localhost:8080/digestion-rules/{id}' \
  -H 'Content-Type: application/json' \
  -d '{"formula":"MUL_ADD:0.01:5"}'

curl -sS -X POST 'http://localhost:8080/digestion-rules/{id}/disable'
curl -sS -X POST 'http://localhost:8080/digestion-rules/{id}/enable'
```

**Priority:** lower number first among matching `eventType`.

---

## Formula

| formula | meaning |
|---------|---------|
| `AMOUNT` | points = amount |
| `RATE:0.01` | amount × 0.01 |
| `FIXED:100` | constant 100 |
| `MUL_ADD:0.01:5` | amount × 0.01 + 5 |
| `{"rate":0.01,"fixed":0}` | same as MUL_ADD (JSON) |

---

## Webhook

Unchanged — see [CLIENT_EARN_WEBHOOK.md](./CLIENT_EARN_WEBHOOK.md).

---

## Phase note

- **P1:** runtime digestion-rules + formula（DB only）  
- **P3 later:** double-entry legs visibility
