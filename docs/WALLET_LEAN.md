# Lean Wallet — ownerId everywhere

Pre-production full cutover. No dual keys.

## Wallet PO

`id · accountId · ownerId · name · type · walletType · status · settlementCurrency`

## API

| Surface | Field |
|---------|--------|
| Onboard `POST /wallets` | `ownerId` |
| Query `GET /wallets/{ownerId}` | path / `?ownerId=` |
| History / as-of | `{ownerId}` |
| Hold / release | body `ownerId` |
| Webhook event | `ownerId` (`associatedIdentifier` JSON alias still accepted once) |
| Failed ingest | column + filter `ownerId` |

## Removed

`alias · hash · nickname · associatedIdentifier` column · `associatedFrom`
