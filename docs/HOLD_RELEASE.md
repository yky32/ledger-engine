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
    "associatedIdentifier": "01A12345678",
    "currency": "LP",
    "amount": 3,
    "movementKey": "hold-order-1"
  }'

curl -sS -X POST 'http://localhost:8080/wallets/releases' \
  -H 'Content-Type: application/json' \
  -d '{
    "associatedIdentifier": "01A12345678",
    "currency": "LP",
    "amount": 3,
    "movementKey": "rel-order-1"
  }'
```

Idempotent by `movementKey`. Order types: `HOLD` / `RELEASE`.
