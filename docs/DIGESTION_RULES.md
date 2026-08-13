# Digestion rules — runtime earn filter + **JSON formula**

**No restart. No YAML seed.**  
Rules live only in DB (`digestion_rule`), managed via **`/digestion-rules`**.

Empty table ⇒ webhook matches nothing (`NO_RULE`) until you create rules via API  
or run [BOOTSTRAP.md](./BOOTSTRAP.md).

---

## Formula config (JSONB) — preferred

`formula` is a **JSON object**, not a cryptic string DSL.

| type | JSON | meaning |
|------|------|---------|
| **AMOUNT** | `{"type":"AMOUNT"}` | points = event amount |
| **RATE** | `{"type":"RATE","rate":0.01}` | points = amount × rate (1%) |
| **FIXED** | `{"type":"FIXED","value":100}` | constant points (ignores amount) |
| **LINEAR** | `{"type":"LINEAR","rate":0.01,"fixed":5}` | amount × rate + fixed |

### Examples (credit-card style)

```json
// 1% cashback on spend
{ "type": "RATE", "rate": 0.01 }

// Card open bonus
{ "type": "FIXED", "value": 1000 }

// 1% + 50 LP promo boost
{ "type": "LINEAR", "rate": 0.01, "fixed": 50 }

// Redeem 1:1 burn
{ "type": "AMOUNT" }
```

### Legacy strings (still accepted on write → normalized to JSON)

`AMOUNT` · `RATE:0.01` · `FIXED:100` · `MUL_ADD:0.01:5` · `{"rate":0.01,"fixed":0}`

---

## Bootstrap

```bash
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
    "formula": { "type": "RATE", "rate": 0.01 }
  }'
```

---

## API

```bash
curl -sS 'http://localhost:8080/digestion-rules'
curl -sS -X PUT "http://localhost:8080/digestion-rules/{id}" \
  -H 'Content-Type: application/json' \
  -d '{"formula":{"type":"RATE","rate":0.05}}'
```

**Priority:** lower number first among matching `eventType`.

---

## Flow

```text
POST/PUT /digestion-rules  →  digestion_rule.formula (jsonb)
                                    │
                                    ▼
                    DigestionFormulaConfig.compute(formula, amount)
                                    │
                                    ▼
                         webhook earn / burn points
```

Code: `DigestionFormulaConfig` · `TransactionRuleEngine`.
