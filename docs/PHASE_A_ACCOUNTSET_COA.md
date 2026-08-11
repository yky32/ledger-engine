# Phase A — AccountSet + multi-currency CoA

**Status:** foundation for double-entry (Phase B)  
**Does not change:** earn still single-leg to **LP AVAILABLE** (legacy poster)

---

## Model

```text
Wallet (associatedIdentifier, settlementCurrency=HKD)
  └── AccountSet code=DEFAULT  status=ACTIVE|FROZEN|CLOSED
        ├── HKD AVAILABLE  (wallet.accountId primary)
        ├── HKD HELD
        ├── HKD ADJUST
        ├── LP  AVAILABLE   ← earn target today
        ├── LP  HELD
        ├── LP  REDEEMED
        ├── LP  EXPIRED
        └── LP  ADJUST
```

Each account: `ledger_balance`, `available_balance`, `account_role`, `status` (ACTIVE|FROZEN|CLOSED|…).

---

## Onboard (upsert)

```bash
curl -sS -X POST 'http://localhost:8080/wallets' \
  -H 'Content-Type: application/json' \
  -d '{
    "associatedIdentifier": "01A12345678",
    "settlementCurrency": "HKD",
    "name": "Customer 01A12345678"
  }'
```

- First call → create wallet + DEFAULT set + 8 books  
- Second call → **upsert** (200, soft-update name/from) — not 409  
- `accounts[]` in body ignored for structure (template is authoritative in Phase A)

## Query

```bash
curl -sS 'http://localhost:8080/wallets/01A12345678'
curl -sS 'http://localhost:8080/wallets/01A12345678?currencies=LP'
```

Response includes:

- `accounts[]` — flat list (filterable by currency)
- `accountSets[]` — nested CoA with `accountRole`

---

## Auto-create webhook

Still gates-first; missing wallet opens **full DEFAULT CoA** (not only HKD+1 LP).

---

## Next

- **Phase B:** double-entry journal (per-currency balanced legs)
- Hold/Burn ops use HELD / REDEEMED roles
