# Lean Wallet PO

Greenfield simplification (no UAT data to migrate).

## Kept

| Field | Role |
|-------|------|
| `id` | PK |
| `accountId` | Primary account |
| `ownerId` | **Unique** CRM / customer id (`associatedIdentifier` in API) |
| `name` | Optional display |
| `status` | Lifecycle |
| `settlementCurrency` | Default settlement book |
| audit / `isActive` | Base entity |

## Removed

| Was | Why |
|-----|-----|
| `alias` | Redundant with ownerId |
| `hash` | No product use |
| `nickname` | → `name` |
| `associatedIdentifier` | Same as ownerId |
| `associatedFrom` | Policy-level label, not wallet identity |
| `type` (CUSTODIAN/CRYPTO) | Always product custodian |
| `walletType` (INDIVIDUAL/CORPORATE) | Unused product branch |

## API

- Request still uses `associatedIdentifier` → stored as `ownerId`
- Response `associatedIdentifier` = `ownerId`
- Lookup: `findByOwnerId` only
