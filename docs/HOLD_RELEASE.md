# Hold / Release

Lock **available** balance without changing **ledger**.

```text
HOLD 3 LP   → available -= 3, ledger unchanged
RELEASE 3   → available += 3 (cannot exceed ledger)
```

## API

```bash
curl -sS -X POST 'http://localhost:8080/wallets/holds' \
  -H 'Content-Type: application/json' \
  -d '{
    "ownerId": "01A12345678",
    "currency": "LP",
    "amount": 3,
    "movementKey": "hold-order-1"
  }'

curl -sS -X POST 'http://localhost:8080/wallets/releases' \
  -H 'Content-Type: application/json' \
  -d '{
    "ownerId": "01A12345678",
    "currency": "LP",
    "amount": 3,
    "movementKey": "rel-order-1"
  }'
```

Idempotent by `movementKey`. Order types: `HOLD` / `RELEASE`.

Entry rows mark `affectsLedger=false` so as-of **ledger** rebuild ignores holds; available still moves.  
See [HISTORY_ASOF_REPLAY.md](./HISTORY_ASOF_REPLAY.md) · [DOUBLE_ENTRY_EARN.md](./DOUBLE_ENTRY_EARN.md).
