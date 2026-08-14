# LedgeRX — docs index

**Product:** [LedgeRX](./BRAND.md) · **Module:** `ledger-engine`

Adopt / ops guides under `docs/`. Code of truth is **main** APIs.

## Start here

| Role | Read |
|------|------|
| **Brand / naming** | [BRAND.md](./BRAND.md) |
| **Business / product review** | [SYSTEM_BUSINESS_FLOW.md](./SYSTEM_BUSINESS_FLOW.md) ⭐ |
| **Credit card / issuer adopt** | [CREDIT_CARD_CLIENT_SCENARIOS.md](./CREDIT_CARD_CLIENT_SCENARIOS.md) ⭐ |
| Fresh local run | [BOOTSTRAP.md](./BOOTSTRAP.md) → `./scripts/upstream-sim.sh` or `e2e-smoke.sh` |
| Concepts (door vs brain) | [INGEST_VS_DIGESTION.md](./INGEST_VS_DIGESTION.md) |
| Wallet onboard | [WALLET_LEAN.md](./WALLET_LEAN.md) | Lean Wallet PO |
| [CLIENT_WALLET_ONBOARDING.md](./CLIENT_WALLET_ONBOARDING.md) |
| Upstream txn → LP | [CLIENT_EARN_WEBHOOK.md](./CLIENT_EARN_WEBHOOK.md) |

## Runtime config (DB — no rule YAML)

| Doc | API / script |
|-----|----------------|
| [INGEST_POLICY.md](./INGEST_POLICY.md) | `GET/PUT /ingest-policies` — kill-switch + auto-wallet |
| [DIGESTION_RULES.md](./DIGESTION_RULES.md) | `/digestion-rules` — filter + formula |
| [BOOTSTRAP.md](./BOOTSTRAP.md) | `./scripts/bootstrap-runtime.sh` |

## Ledger ops

| Doc | API |
|-----|-----|
| [DOUBLE_ENTRY_EARN.md](./DOUBLE_ENTRY_EARN.md) | PROGRAM DE legs · `GET /integrations/ledger-entries` |
| [HOLD_RELEASE.md](./HOLD_RELEASE.md) | `POST /wallets/holds` · `/releases` |
| [HISTORY_ASOF_REPLAY.md](./HISTORY_ASOF_REPLAY.md) | movements · as-of · fail replay · pagination |

## Task archive

| Doc | Note |
|-----|------|
| [HERMES_RUNTIME_RULES_TASK.md](./HERMES_RUNTIME_RULES_TASK.md) | Original brief — **DONE** |

## Conventions

- **No `/by-*` path segments** — filters are query params (`?eventId=`, `?code=`).
- **Pagination** (list APIs): 1-based `page` / `size` + optional `startDt` / `endDt` (see HISTORY_ASOF_REPLAY).
- **1 CUST → 1 Wallet**; multi-ccy = accounts under that wallet.
- Root product docs: [../PRODUCT.md](../PRODUCT.md) · [../INTEGRATION.md](../INTEGRATION.md) · [../README.md](../README.md)
| [COA_PROFILE.md](./COA_PROFILE.md) | 1-table COA `/coa-profiles` |
| [TECH_DEBT.md](./TECH_DEBT.md) | Deferred work — API key later, migrations, legacy API |
