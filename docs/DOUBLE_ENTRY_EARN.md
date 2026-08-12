# Double-entry earn (PROGRAM pool)

Earn/burn loyalty points post **two balanced legs** per currency:

```text
EARN N LP
  DEBIT  PROGRAM pool LP   N
  CREDIT customer LP       N

BURN N LP
  DEBIT  customer LP       N
  CREDIT PROGRAM pool LP   N
```

| | |
|--|--|
| PROGRAM owner id | `PROGRAM` (lazy bootstrap on first earn/burn) |
| Pool account | `allowNegative=true` |
| Product path | Digestion → `IngestTransactionUseCase` → `LedgerMovementShooter` |

Related: [CLIENT_EARN_WEBHOOK.md](./CLIENT_EARN_WEBHOOK.md) · [INGEST_VS_DIGESTION.md](./INGEST_VS_DIGESTION.md)

---

## Webhook response

```json
{
  "status": "EARNED",
  "points": 1,
  "movementId": 123,
  "legs": [
    { "entryId": 1, "accountId": 99, "direction": "DEBIT",  "amount": 1, "currency": "LP" },
    { "entryId": 2, "accountId": 88, "direction": "CREDIT", "amount": 1, "currency": "LP" }
  ]
}
```

---

## Query legs

Provide **exactly one** of `movementId` or `eventId` (query params only — no `/by-*` paths):

```bash
curl -sS 'http://localhost:8080/integrations/ledger-entries?movementId=123'
curl -sS 'http://localhost:8080/integrations/ledger-entries?eventId=txn-1'
curl -sS 'http://localhost:8080/integrations/ledger-entries?eventId=txn-1&operation=earn'
```

HOLD/RELEASE also write entry rows; they set `affectsLedger=false` so **as-of ledger** ignores them (available still moves). See [HISTORY_ASOF_REPLAY.md](./HISTORY_ASOF_REPLAY.md) · [HOLD_RELEASE.md](./HOLD_RELEASE.md).
