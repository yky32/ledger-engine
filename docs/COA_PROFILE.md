# COA profile — flat 1-table (no JSONB)

**Table:** `coa_profile`  
**API:** `/coa-profiles`  
**Onboard:** `POST /wallets` field `coaProfileCode` (optional → DEFAULT)

One row = one **product stream** (UAF: entity is stream, not legal entity).

| Column | Default | Meaning |
|--------|---------|---------|
| `code` | `DEFAULT` | unique key used by onboard |
| `entity` | `10` | **product stream** segment (UAF: `01`=CC, `02`=Loan) |
| `type` | `20` | account type |
| `sub_type` | `00` | sub-type |
| `buffer` | `00` | buffer |
| `lp_currency` | `LP` | points currency label |
| `pool_allow_negative` | `true` | PROGRAM pool |

Wallet stamps `wallet.coaProfileCode` at onboard. Extra books (ensure LP) reuse that profile.

## UAF example

```text
POST /coa-profiles  { "code":"UAF_CC",   "entity":"01", "type":"20", "subType":"00", "buffer":"00" }
POST /coa-profiles  { "code":"UAF_LOAN", "entity":"02", "type":"20", "subType":"00", "buffer":"00" }

POST /wallets {
  "ownerId": "UAF-CARD-10086",
  "settlementCurrency": "HKD",
  "coaProfileCode": "UAF_CC",
  "accounts": [ { "primary": true }, { "currency": "LP" } ]
}
→ accounts fullNumber start with entity 01-…

POST /wallets {
  "ownerId": "UAF-LOAN-7788",
  "settlementCurrency": "HKD",
  "coaProfileCode": "UAF_LOAN"
}
→ entity 02-…
```

Same person card+loan → **two ownerIds** (1 ownerId → 1 wallet rule).

## API

```bash
curl -sS localhost:8080/coa-profiles/default
curl -sS -X POST localhost:8080/coa-profiles -H 'Content-Type: application/json' \
  -d '{"code":"UAF_CC","name":"UAF Credit Card","entity":"01","type":"20","subType":"00","buffer":"00"}'
curl -sS -X POST localhost:8080/wallets -H 'Content-Type: application/json' \
  -d '{"ownerId":"UAF-CARD-1","settlementCurrency":"HKD","coaProfileCode":"UAF_CC"}'
```
