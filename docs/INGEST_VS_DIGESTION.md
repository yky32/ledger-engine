# Ingest vs Digestion

Two **different** product concepts — packages are split the same way.

| | **Ingest** (door) | **Digestion** (brain) |
|--|-------------------|------------------------|
| Meaning | Take event **into** the system | **Interpret / score** the event |
| Package | `…po.ingest` · `…usecase.ingest` · `…endpoint.ingest` | `…po.digestion` · `…usecase.digestion` · `…endpoint.digestion` |
| Main type | `IngestPolicy` | `DigestionRule` |
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
   wallet + DE earn/burn
```

Do **not** merge these into one table or one package name.
