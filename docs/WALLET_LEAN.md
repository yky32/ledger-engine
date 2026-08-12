# Lean Wallet PO

Greenfield simplification (pre-UAT).

## Fields

| Field | Role |
|-------|------|
| `id` | PK |
| `accountId` | Primary account |
| `ownerId` | **Unique** CRM / customer id — **all wallet queries** |
| `name` | Optional display |
| `type` | `WalletAssociationType` (default `CUSTODIAN`) |
| `walletType` | `WalletType` (default `INDIVIDUAL` product / `CORPORATE` ledger attach) |
| `status` | Lifecycle |
| `settlementCurrency` | Default settlement book |
| audit / `isActive` | Base |

## Removed

`alias` · `hash` · `nickname` · DB column `associated_identifier` · `associatedFrom`

## Query = ownerId only

```text
GET /wallets/{ownerId}
GET /wallets?ownerId=
GET /wallets/{ownerId}/movements
GET /wallets/{ownerId}/balances/as-of
POST /wallets/holds   body: { ownerId, ... }
POST /wallets/releases body: { ownerId, ... }
```

## Create / ingest

- Onboard still accepts `associatedIdentifier` → stored as `ownerId`
- Webhook event field remains `associatedIdentifier` (upstream CRM id) → maps to wallet `ownerId`
