# History + as-of + replay polish + concurrency

## Pagination (tgt.profile style)

All list endpoints use **1-based** Spring `Pageable`:

```text
@PageableDefault(page = 1, size = Integer.MAX_VALUE, sort = "createDt", direction = DESC)
Pageable pageable
@RequestParam(required = false) String startDt   // ISO-8601 or yyyy-MM-dd
@RequestParam(required = false) String endDt
```

Use case converts via `Pageables.toZeroBased(pageable)` before JPA.
Response: `R.success(list, Pagination.create(page))` → `data[]` + `pagination{currentPage,pageSize,total,…}`.

## Movement history

```bash
curl -sS "http://localhost:8080/wallets/{CUST}/movements?orderType=HOLD&currency=LP&page=1&size=20"
curl -sS "http://localhost:8080/wallets/{CUST}/movements?startDt=2026-01-01&endDt=2026-12-31"
```

## Balance as-of

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
