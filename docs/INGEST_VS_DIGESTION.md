# Ingest vs Digestion

Two **different** product concepts — Java packages are split the same way.

| | **Ingest** (door) | **Digestion** (brain) |
|--|-------------------|------------------------|
| Meaning | Take event **into** the system | **Interpret / score** the event |
| Package | `…po.ingest` · `…usecase.ingest` · `…endpoint.ingest` | `…po.digestion` · `…usecase.digestion` · `…endpoint.digestion` |
| Main type | `IngestPolicy` | `DigestionRule` |
| Table | `ingest_policy` | `digestion_rule` |
| API | `/ingest-policy`, webhooks, fail-table, legs | `/digestion-rules` |
| Cardinality | ~1 policy row | N rules |

```text
POST /integrations/webhooks/transactions
        │
        ▼
   IngestPolicy          ← door (enabled? auto-wallet?)
        │
        ▼
   DigestionRule[]       ← brain (match + formula → points)
        │
        ▼
   wallet + PROGRAM DE earn/burn + legs
```

| Doc | |
|-----|--|
| [INGEST_POLICY.md](./INGEST_POLICY.md) | Door fields + example |
| [DIGESTION_RULES.md](./DIGESTION_RULES.md) | Rules + formulas |
| [CLIENT_EARN_WEBHOOK.md](./CLIENT_EARN_WEBHOOK.md) | Upstream curl playbook |

Do **not** merge these into one table or one package name.
