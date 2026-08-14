# LedgeRX API surface

> **Docs entry:** [START_HERE.md](./START_HERE.md) — 唔好從本檔開始亂跳。

**In-cluster only.** See [TECH_DEBT.md](./TECH_DEBT.md).

## Product APIs (use these)

| Area | Base path |
|------|-----------|
| Wallet onboard / query | `/wallets` |
| Movements (deposit/withdraw/transfer) | `/movements` |
| Hold / release | `/wallets/holds`, `/wallets/releases` |
| History / as-of | `/wallets/{ownerId}/movements`, balances as-of |
| Door | `/ingest-policies` |
| Brain | `/digestion-rules` |
| Webhook + dry-run | `/integrations/webhooks/transactions` (+ `/dry-run`) |
| Fail queue | `/integrations/failed-transactions` |
| DE legs | `/integrations/ledger-entries` |
| COA (internal config) | `/coa-profiles` |
| System config | `/configurations` |

## Legacy catalog (deprecated — still served)

Do **not** use for new LedgeRX product work. Controllers marked `@Deprecated`.

| Path | Prefer instead |
|------|----------------|
| `/ledger-wallets` | `/wallets` |
| `/ledger-accounts`, `/accounts` | wallet onboard + `/coa-profiles` |
| `/ledger-accounts/movements` | `/movements` · `/wallets/{ownerId}/movements` |
| `/rules`, `/rule-executions` | `/digestion-rules` · Review / fail queue |

Admin portal no longer ships pages for these; old bookmarks redirect to product screens.
