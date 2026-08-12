# History + as-of + replay polish + concurrency

## Movement history

```bash
curl -sS "http://localhost:8080/wallets/{CUST}/movements?orderType=HOLD&currency=LP&page=0&size=20"
# optional: status=SETTLED&from=2026-01-01T00:00:00Z&to=2026-12-31T23:59:59Z
```

## Balance as-of

Rebuilds from `ledger_entry` with `affectsLedger` / `affectsAvailable` flags
(HOLD legs affect available only).

```bash
curl -sS "http://localhost:8080/wallets/{CUST}/balances/as-of?currency=LP"
curl -sS "http://localhost:8080/wallets/{CUST}/balances/as-of?at=2026-08-12T00:00:00Z&currency=LP"
```

## Fail replay polish

- Replay does **not** insert a second fail row when still SKIPPED
- Bulk:

```bash
curl -sS -X POST 'http://localhost:8080/integrations/failed-transactions/replay' \
  -H 'Content-Type: application/json' \
  -d '{"ids":[1,2,3]}'
```
