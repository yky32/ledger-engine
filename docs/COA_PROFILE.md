# COA profile — flat 1-table (no JSONB)

**Table:** `coa_profile`  
**API:** `/coa-profiles`

One row per client. Shared segment codes for all member books.

| Column | Default (legacy) | Meaning |
|--------|------------------|---------|
| `code` | `DEFAULT` | unique |
| `entity` | `10` | entity segment |
| `type` | `20` | account type (LIABILITY) |
| `sub_type` | `00` | sub-type |
| `buffer` | `00` | buffer |
| `lp_currency` | `LP` | points currency label |
| `pool_allow_negative` | `true` | PROGRAM pool |

`mainAccount` / `subAccount` still from wallet sequence.

## API

```bash
curl -sS localhost:8080/coa-profiles/default
curl -sS -X POST localhost:8080/coa-profiles -H 'Content-Type: application/json' -d '{
  "code":"BANK_A",
  "name":"Bank A",
  "entity":"01",
  "type":"99",
  "subType":"00",
  "buffer":"00"
}'
curl -sS -X PUT localhost:8080/coa-profiles/{id} -H 'Content-Type: application/json' -d '{
  "entity":"01","type":"99"
}'
```

Onboard uses **default** profile for fullNumber segments.
