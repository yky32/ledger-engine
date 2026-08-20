# Factors — Door entry & Brain when (P1/P2)

> **Docs entry:** [START_HERE.md](./START_HERE.md)

Common **factor** predicates apply to:

1. **Ingest (Door)** — `entryFactors` → is the event **entered**?
2. **Digest (Brain)** — `whenFactors` (+ legacy columns) → which **equation / multiplier**?

Scoring never runs on the Door.

## Shape

```json
{ "field": "currency", "op": "in", "value": ["HKD", "USD"] }
{ "field": "amount", "op": "between", "value": { "min": 1, "max": 10000 } }
{ "field": "ageDays", "op": "lte", "value": 30 }
{ "field": "mcc", "op": "nin", "value": ["6010"] }
{ "field": "metadata.channel", "op": "eq", "value": "POS" }
```

### Fields

| field | Notes |
|-------|--------|
| `currency` / `ccy` | ISO from event |
| `mcc` | metadata `mcc` / `mccCode` / `merchantCategoryCode` |
| `amount` | event amount |
| `ageDays` | days since `occurredAt` |
| `eventType` | |
| `metadata.<key>` | string metadata |

### Ops

`eq` · `neq` · `in` · `nin` · `gt` · `gte` · `lt` · `lte` · `between` · `exists`

`between` value: `{ "min": …, "max": … }` or `[min, max]`.

## Door — `PUT /ingest-policies`

```json
{
  "isEnabled": true,
  "entryFactors": [
    { "field": "currency", "op": "in", "value": ["HKD", "USD"] },
    { "field": "amount", "op": "gt", "value": 0 }
  ]
}
```

- Empty / null → only `isEnabled`
- Fail → status skip · reason `NOT_ENTERED` · trace rule `_DOOR_`

## Brain — digestion rule

Legacy still work (compiled at runtime):

- `minAmount` → `amount gte`
- `eligibleCurrencies` → `currency in`
- `eligibleMccs` → `mcc in`
- `maxAgeDays` → `ageDays lte`

Plus explicit:

```json
{
  "code": "GROCERY_2X",
  "eventType": "PURCHASE",
  "operation": "EARN",
  "whenFactors": [
    { "field": "mcc", "op": "in", "value": ["5411"] },
    { "field": "amount", "op": "between", "value": { "min": 100, "max": 999999 } }
  ],
  "formula": { "type": "RATE", "rate": 0.01, "multiplier": 2 }
}
```

Legacy columns **AND** `whenFactors` (all must pass).

## Equation multiplier

Optional on formula: `"multiplier": 2` (alias `mult`) → base points × multiplier.

## Flow

```text
Event → Door isEnabled → entryFactors → entered?
      → Brain rules (priority) → whenFactors (+legacy) → formula(+mult) → Books
```

## Code

- `usecase/factor/FactorMatcher.java`
- `usecase/factor/FactorSpec.java`
- DigestionRule.whenFactors · IngestPolicy.entryFactors (JSONB)
