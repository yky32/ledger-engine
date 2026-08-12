# Ledger Engine — docs index

Adopt / ops guides under `docs/`. Code of truth is **main** APIs; these pages track product surface after PR **#7–#17**.

## Start here

| Role | Read |
|------|------|
| **Business / product review** | [SYSTEM_BUSINESS_FLOW.md](./SYSTEM_BUSINESS_FLOW.md) ⭐ |
| Fresh local run | [BOOTSTRAP.md](./BOOTSTRAP.md) → `./scripts/upstream-sim.sh` or `e2e-smoke.sh` |
| Concepts (door vs brain) | [INGEST_VS_DIGESTION.md](./INGEST_VS_DIGESTION.md) |
| Wallet onboard | [WALLET_LEAN.md](./WALLET_LEAN.md) | Lean Wallet PO |
| [CLIENT_WALLET_ONBOARDING.md](./CLIENT_WALLET_ONBOARDING.md) |
| Upstream txn → LP | [CLIENT_EARN_WEBHOOK.md](./CLIENT_EARN_WEBHOOK.md) |

## Runtime config (DB — no rule YAML)

| Doc | API / script |
|-----|----------------|
| [INGEST_POLICY.md](./INGEST_POLICY.md) | `GET/PUT /ingest-policy` — kill-switch + auto-wallet |
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
