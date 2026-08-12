# History + as-of + fail replay + concurrency

## Pagination (tgt.profile style)

List endpoints use **1-based** Spring `Pageable`:

```text
@PageableDefault(page = 1, size = Integer.MAX_VALUE, sort = "createDt", direction = DESC)
Pageable pageable
@RequestParam(required = false) String startDt   // ISO-8601 or yyyy-MM-dd
@RequestParam(required = false) String endDt
```

Use cases convert via `Pageables.toZeroBased(pageable)` before JPA.  
Response: `R.success(list, Pagination.create(page))` → `data[]` + `pagination{currentPage,pageSize,total,…}`.

Applies to: wallet movements, failed-transactions search, ledger movement lists, fx-rates, rules, etc.

---

## Movement history

```bash
curl -sS "http://localhost:8080/wallets/{CUST}/movements?orderType=HOLD&currency=LP&page=1&size=20"
curl -sS "http://localhost:8080/wallets/{CUST}/movements?startDt=2026-01-01&endDt=2026-12-31&status=SETTLED"
```

Optional filters: `orderType`, `currency`, `status`, `startDt`, `endDt`.

---

## Balance as-of

Rebuilds from `ledger_entry` using `affectsLedger` / `affectsAvailable` (HOLD legs skip ledger).

```bash
curl -sS "http://localhost:8080/wallets/{CUST}/balances/as-of?currency=LP"
curl -sS "http://localhost:8080/wallets/{CUST}/balances/as-of?at=2026-08-12T00:00:00Z&currency=LP"
```

---

## Fail replay

- Single replay does **not** insert a second fail row when still SKIPPED
- Status → `REPLAYED` on EARNED/BURNED/DUPLICATE

```bash
curl -sS -X POST 'http://localhost:8080/integrations/failed-transactions/{id}/review'
curl -sS -X POST 'http://localhost:8080/integrations/failed-transactions/{id}/replay'

# bulk max 50
curl -sS -X POST 'http://localhost:8080/integrations/failed-transactions/replay' \
  -H 'Content-Type: application/json' \
  -d '{"ids":[1,2,3]}'
```

List (pageable, not `limit`):

```bash
curl -sS 'http://localhost:8080/integrations/failed-transactions?status=OPEN&page=1&size=20'
```

---

## Concurrency / idempotency (engine behavior)

- Account updates under row lock; concurrent HOLDs that exceed **available** → one wins, others `MOV0403`
- Same `movementKey` on hold/earn → idempotent (second call returns existing movement)

ITs: `ConcurrencyReplayAsOfIntegrationTest`
