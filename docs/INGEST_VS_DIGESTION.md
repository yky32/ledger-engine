# Ingest vs Digestion

Two **different** product concepts — keep packages and ops screens separate.

| | **Ingest policy (Door)** | **Digestion rules (Brain)** |
|--|--------------------------|----------------------------|
| Meaning | Accept traffic? Auto-open wallet? | **Is this txn eligible for a rule?** + **how many points?** |
| Package | `…po.ingest` · `…usecase.ingest` | `…po.digestion` · `…usecase.digestion` |
| Main type | `IngestPolicy` | `DigestionRule` |
| Table | `ingest_policy` | `digestion_rule` |
| API | `/ingest-policies`, webhooks, fail-table, legs | `/digestion-rules` |
| Cardinality | ~1 policy row | N rules |
| Checks | `isEnabled`, auto-wallet ccy/prefix | eventType, minAmount, **currency**, **MCC**, **age**, formula |

```text
POST /integrations/webhooks/transactions
        │
        ▼
   IngestPolicy              ← door ONLY (enabled? later auto-wallet?)
        │
        ▼
   DigestionRule[]           ← brain: ELIGIBILITY filters then FORMULA
        │  currency / mcc / age / minAmount / eventType
        │  → formula JSON → points
        ▼
   ensure wallet (door auto-create if needed)
        │
        ▼
   PROGRAM DE earn/burn + legs
```

### Where to configure “sanity”

| Question | Where |
|----------|--------|
| System still accepting webhooks? | **Door** `isEnabled` |
| First-time member auto wallet? | **Door** `isAutoCreateWallet` + settlement/ensure ccy |
| This purchase currency allowed? | **Brain** `eligibleCurrencies` |
| This MCC allowed? | **Brain** `eligibleMccs` + webhook `metadata.mcc` |
| Txn too old? | **Brain** `maxAgeDays` |
| Min spend? | **Brain** `minAmount` |
| Which % / fixed points? | **Brain** `formula` |

Do **not** put currency/MCC/age on Door. Do **not** merge into one table.

| Doc | |
|-----|--|
| [INGEST_POLICY.md](./INGEST_POLICY.md) | Door fields |
| [DIGESTION_RULES.md](./DIGESTION_RULES.md) | Eligibility + formula |
| [CLIENT_EARN_WEBHOOK.md](./CLIENT_EARN_WEBHOOK.md) | Upstream curl |
| [CREDIT_CARD_CLIENT_SCENARIOS.md](./CREDIT_CARD_CLIENT_SCENARIOS.md) | Issuer scenarios |
