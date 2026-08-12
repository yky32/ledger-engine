# Lean Wallet — ownerId everywhere

Pre-production full cutover. No dual keys.

## Wallet PO

`id · accountId · ownerId · vanityCode · name · type · walletType · status · settlementCurrency`

| Field | Role |
|-------|------|
| `id` | Snowflake PK — internal only |
| `ownerId` | CRM / integration key |
| `vanityCode` | Optional customer-facing display (lucky/premium). **Not** identity. |

## Vanity

- Helper: `com.altech.ledger.util.WalletVanityCodes`
  - `generatePlaceholder(ownerId)` — **TODO** real pool / 8888 rules (returns `null` for now)
  - `normalize` / `resolveForCreate`
- Create: optional request `vanityCode`; else placeholder
- Unique when set (`uk_wallet_vanity_code`)
- Never use as FK / movement key / webhook key

## API

| Surface | Field |
|---------|--------|
| Onboard `POST /wallets` | `ownerId` (+ optional `vanityCode`) |
| Query `GET /wallets/{ownerId}` | path / `?ownerId=` |
| History / as-of | `{ownerId}` |
| Hold / release | body `ownerId` |
| Webhook event | `ownerId` |
| Failed ingest | `ownerId` |
