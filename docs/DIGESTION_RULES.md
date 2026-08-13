# Digestion rules — Brain: **eligibility + formula**

**No restart. No YAML seed.**  
Rules live only in DB (`digestion_rule`), managed via **`/digestion-rules`**.

Empty table ⇒ webhook matches nothing (`NO_RULE`) until you create rules via API  
or run [BOOTSTRAP.md](./BOOTSTRAP.md).

---

## Mental model (read this first)

Each **DigestionRule** row does **two jobs** in order:

```text
1) ELIGIBILITY (sanity / match filters)
   eventType → minAmount → eligibleCurrencies → eligibleMccs → maxAgeDays
        │
        ▼  only if all pass
2) FORMULA (equation → points)
   {"type":"RATE","rate":0.01}  etc.
```

| Layer | Config fields | Skip codes |
|-------|---------------|------------|
| **Eligibility** | `eventType`, `minAmount`, `eligibleCurrencies`, **`eligibleMccs`**, `maxAgeDays`, `priority` | `CURRENCY`, `MCC`, `AGE`, `MIN_AMOUNT`, `AMOUNT`, `NO_RULE` |
| **Formula** | `formula` (JSONB), `operation`, `pointCurrency` | `FORMULA`, `POINTS` |

**Door (`/ingest-policies`)** does **not** run these checks — only enabled + auto-wallet.  
See [INGEST_VS_DIGESTION.md](./INGEST_VS_DIGESTION.md).

**Priority:** lower number wins among candidates; **first full match** stops the loop.

---

## Eligibility filters

| Field | Meaning | Empty / null |
|-------|---------|----------------|
| `eventType` | Must equal webhook `eventType` (ignore case) | required |
| `minAmount` | `event.amount >= minAmount` | default 0 |
| `eligibleCurrencies` | Allow-list ISO codes | **any** currency |
| **`eligibleMccs`** | Allow-list MCC codes | **any** MCC |
| `maxAgeDays` | `occurredAt` not older than N days | no age check |

### MCC (added)

Webhook must send MCC in **metadata** (string map), one of:

- `metadata.mcc`
- `metadata.mccCode`
- `metadata.merchantCategoryCode`

Example grocery-only 3%:

```json
{
  "code": "PURCHASE_GROCERY",
  "eventType": "PURCHASE",
  "operation": "EARN",
  "priority": 10,
  "eligibleCurrencies": ["HKD", "USD"],
  "eligibleMccs": ["5411"],
  "maxAgeDays": 7,
  "minAmount": 0.01,
  "pointCurrency": "LP",
  "formula": { "type": "RATE", "rate": 0.03 }
}
```

```json
{
  "eventId": "…",
  "ownerId": "01A…",
  "eventType": "PURCHASE",
  "amount": 300,
  "currency": "HKD",
  "occurredAt": "2026-08-13T10:00:00Z",
  "metadata": { "mcc": "5411", "merchantName": "PARKnSHOP" }
}
```

If rule has `eligibleMccs` set and event has **no** mcc → skip **`MCC`**.

---

## Formula config (JSONB)

| type | JSON | meaning |
|------|------|---------|
| **AMOUNT** | `{"type":"AMOUNT"}` | points = event amount |
| **RATE** | `{"type":"RATE","rate":0.01}` | amount × rate |
| **FIXED** | `{"type":"FIXED","value":100}` | constant |
| **LINEAR** | `{"type":"LINEAR","rate":0.01,"fixed":5}` | amount×rate+fixed |

Legacy strings still accepted on write → normalized to JSON.

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
    "eligibleMccs": [],
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
  -d '{"eligibleMccs":["5411","5812"],"formula":{"type":"RATE","rate":0.03}}'
```

---

## Flow

```text
POST/PUT /digestion-rules  →  digestion_rule (filters + formula jsonb)
                                    │
                                    ▼
                    TransactionRuleEngine.evaluate(event)
                      eligibility → DigestionFormulaConfig.compute
                                    │
                                    ▼
                         webhook earn / burn points
```

Code: `TransactionRuleEngine` · `DigestionFormulaConfig` · `DigestionRuleUseCase`.

---

## Trust pack B — evaluate trace + dry-run

Live webhook and dry-run return:

| Field | Meaning |
|-------|---------|
| `matchedRuleCode` | Winning `DigestionRule.code` |
| `eligibilityTrace[]` | Candidate rules (same eventType): `ruleCode`, `priority`, `matched`, `failStep`, `detail` |
| `dryRun` | `true` only on dry-run path |

```bash
# no wallet / no books
curl -sS -X POST 'http://localhost:8080/integrations/webhooks/transactions/dry-run' \
  -H 'Content-Type: application/json' \
  -d '{
    "eventId":"dry-1","ownerId":"01A1","eventType":"PURCHASE",
    "amount":500,"currency":"HKD","occurredAt":"2026-08-13T10:00:00Z",
    "metadata":{"mcc":"5411"}
  }'
```

`failStep`: `AMOUNT` · `MIN_AMOUNT` · `CURRENCY` · `MCC` · `AGE` · `FORMULA` · `POINTS` · `BAD_RULE`
