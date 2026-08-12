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

- PROGRAM wallet owner id: `PROGRAM` (lazy bootstrap)
- Pool account: `allowNegative=true` (liability/expense side can go negative)

## Webhook response

```json
{
  "status": "EARNED",
  "points": 1,
  "movementId": 123,
  "legs": [
    { "entryId": 1, "accountId": 99, "direction": "DEBIT", "amount": 1, "currency": "LP" },
    { "entryId": 2, "accountId": 88, "direction": "CREDIT", "amount": 1, "currency": "LP" }
  ]
}
```

## Query legs

Provide **exactly one** of `movementId` or `eventId`:

```bash
curl -sS "http://localhost:8080/integrations/ledger-entries?movementId=123"
curl -sS "http://localhost:8080/integrations/ledger-entries?eventId=txn-1"
curl -sS "http://localhost:8080/integrations/ledger-entries?eventId=txn-1&operation=earn"
```
