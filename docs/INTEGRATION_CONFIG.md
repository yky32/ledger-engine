# Integration config (DB runtime)

Business flags for webhook / auto-wallet live in **`integration_config`** (not YAML catalogs).

```bash
# read (creates default row from code/env fallback if empty)
curl -sS 'http://localhost:8080/integrations/config'

# update (takes effect next request — no restart)
curl -sS -X PUT 'http://localhost:8080/integrations/config' \
  -H 'Content-Type: application/json' \
  -d '{
    "isEnabled": true,
    "isAutoCreateWallet": true,
    "autoWalletSettlementCurrency": "HKD",
    "autoWalletEnsureCurrency": "LP",
    "autoWalletAssociatedFrom": "CRM",
    "autoWalletNamePrefix": "Auto "
  }'
```

| Field | Meaning |
|-------|---------|
| `isEnabled` | Webhook integration on/off |
| `isAutoCreateWallet` | After gates, provision missing wallet |
| `autoWalletSettlementCurrency` | Primary book (default HKD) |
| `autoWalletEnsureCurrency` | Extra book (default LP) |
| `autoWalletAssociatedFrom` | association source label |
| `autoWalletNamePrefix` | nickname prefix |

First GET/PUT (or first earn) materializes one active row. Env `ledger.integration.*` only seeds that first row.
