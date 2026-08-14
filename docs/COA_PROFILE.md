# COA profile — flat 1-table (no JSONB)

**Product:** LedgeRX (standalone)  
**Table:** `coa_profile`  
**API:** `/coa-profiles`  
**Onboard:** `POST /wallets` optional `coaProfileCode` (omit → DEFAULT)

One row = one COA scheme / product line the operator defines.  
`entity` is a **numeric segment** in `fullNumber` — meaning is defined by your COA catalog, not hard-coded clients.

| Column | Default | Meaning |
|--------|---------|---------|
| `code` | `DEFAULT` | unique key used by onboard |
| `entity` | `10` | entity segment |
| `type` | `20` | account type segment |
| `sub_type` | `00` | sub-type |
| `buffer` | `00` | buffer |
| `lp_currency` | `LP` | points currency label |
| `pool_allow_negative` | `true` | PROGRAM pool |

Wallet stamps `wallet.coaProfileCode` at onboard. Extra books (ensure LP) reuse that profile.

## Door lazy onboard COA

`ingest_policy.auto_wallet_coa_profile_code` — when webhook auto-creates a wallet.

Priority:

1. `metadata.coaProfileCode`
2. Door `autoWalletCoaProfileCode`
3. DEFAULT

## Example (generic)

```bash
# custom scheme
curl -sS -X POST localhost:8080/coa-profiles -H 'Content-Type: application/json' -d '{
  "code":"STREAM_A",
  "name":"Product line A",
  "entity":"01",
  "type":"20",
  "subType":"00",
  "buffer":"00"
}'

# onboard onto that scheme
curl -sS -X POST localhost:8080/wallets -H 'Content-Type: application/json' -d '{
  "ownerId":"CUST-10086",
  "settlementCurrency":"HKD",
  "coaProfileCode":"STREAM_A",
  "accounts":[{ "currency":"LP" }]
}'
```

```bash
curl -sS localhost:8080/coa-profiles/default
curl -sS localhost:8080/coa-profiles
```
