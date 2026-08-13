# COA profile — simple 1-table design

**Table:** `coa_profile`  
**API:** `/coa-profiles`  
**Idea:** one row per client; `bindings` JSONB holds 3 roles. No entity/type dictionary tables.

## Roles

| Role | Use |
|------|-----|
| `MEMBER_SETTLEMENT` | Member settlement book (HKD…) |
| `MEMBER_LP` | Member points book |
| `PROGRAM_POOL` | Earn/burn counterparty (`allowNegative`) |

## Binding fields

```json
{
  "entity": "10",
  "type": "20",
  "subType": "00",
  "buffer": "00",
  "currencyMode": "SETTLEMENT|ENSURE|FIXED",
  "currency": "LP",
  "allowNegative": false
}
```

`mainAccount` / `subAccount` still from wallet sequence (not in JSON).

## API

```bash
curl -sS localhost:8080/coa-profiles
curl -sS localhost:8080/coa-profiles/default   # lazy seed DEFAULT
curl -sS -X POST localhost:8080/coa-profiles -H 'Content-Type: application/json' -d '{
  "code":"BANK_A",
  "name":"Bank A",
  "bindings":{
    "MEMBER_LP":{"entity":"01","type":"99","subType":"00","buffer":"00","currencyMode":"FIXED","currency":"LP"}
  }
}'
```

DEFAULT seed = legacy `CoaCodes` (entity 10, LIABILITY 20, …). Onboard / ensure-LP read default profile.
