# Factors — Door entry & Brain when

> **Docs entry:** [START_HERE.md](./START_HERE.md)

Common **factor** predicates apply to:

1. **Ingest (Door)** — `entryFactors` → is the event **entered**?
2. **Digest (Brain)** — `whenFactors` (+ legacy columns) → which **equation / multiplier**?

Scoring never runs on the Door.

## Leaf shape

```json
{ "field": "currency", "op": "in", "value": ["HKD", "USD"] }
{ "field": "amount", "op": "between", "value": { "min": 1, "max": 10000 } }
{ "field": "ageDays", "op": "lte", "value": 30 }
{ "field": "mcc", "op": "nin", "value": ["6010"] }
{ "field": "metadata.channel", "op": "eq", "value": "POS" }
```

Optional `"id"` on a leaf helps `atLeast` / group failure messages.

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

---

## FactorSet — boolean composition (UAF)

Plain **array** = **AND all** (compat).

Object form supports the three UAF patterns:

### 1) Any one of N (`match: any`)

```json
{
  "match": "any",
  "factors": [ F1, F2, F3, F4, F5 ]
}
```

### 2) At least K of N (`match: atLeast`)

```json
{
  "match": "atLeast",
  "count": 2,
  "factors": [ F1, F2, F3, F4, F5 ]
}
```

Aliases: `min` / `minMatch` / `atLeast` for count.

### 3) Specific combinations (`match: anyGroup`)

**(F1∧F2) ∨ (F3∧F4)** — e.g. ccy+mcc **or** amount band:

```json
{
  "match": "anyGroup",
  "groups": [
    { "id": "G12", "factors": [ F1, F2 ] },
    { "id": "G34", "factors": [ F3, F4 ] }
  ]
}
```

Each group defaults to **AND**. Nested sets allowed (group can set `"match":"any"` etc.).

`allGroups` = every group must pass.

### Nested

Children of a set may be leaves **or** nested FactorSets.

---

## Door — `PUT /ingest-policies`

```json
{
  "isEnabled": true,
  "entryFactors": {
    "match": "any",
    "factors": [
      { "field": "currency", "op": "in", "value": ["HKD", "USD"] },
      { "field": "metadata.channel", "op": "eq", "value": "POS" }
    ]
  }
}
```

- Empty / null → only `isEnabled`
- Fail → `NOT_ENTERED` · trace `_DOOR_` · failStep often `SET`

## Brain — digestion rule

Legacy still compile to leaves at runtime:

- `minAmount` → `amount gte`
- `eligibleCurrencies` → `currency in`
- `eligibleMccs` → `mcc in`
- `maxAgeDays` → `ageDays lte`

Plus `whenFactors` array **or** FactorSet. Legacy **AND** explicit set.

```json
{
  "code": "UAF_COMBO_EARN",
  "eventType": "PURCHASE",
  "whenFactors": {
    "match": "anyGroup",
    "groups": [
      { "id": "retail", "factors": [
          { "field": "mcc", "op": "in", "value": ["5411","5812"] },
          { "field": "currency", "op": "eq", "value": "HKD" }
      ]},
      { "id": "bigTicket", "factors": [
          { "field": "amount", "op": "gte", "value": 5000 }
      ]}
    ]
  },
  "formula": { "type": "RATE", "rate": 0.01, "multiplier": 2 }
}
```

## Equation multiplier

`"multiplier": 2` on formula → base points × mult.

## Flow

```text
Event → Door isEnabled → entryFactors (leaf|set) → entered?
      → Brain → whenFactors (+legacy AND) → formula(+mult) → Books
```

## Code

- `FactorMatcher` · `FactorSpec`
- PO: `entryFactors` / `whenFactors` JSONB **Object** (array or set)
