# LedgeRX — Tech debt & deferred work

> **Docs entry:** [START_HERE.md](./START_HERE.md) — 唔好從本檔開始亂跳。

**Product:** LedgeRX · module `ledger-engine`  
**Deploy assumption:** **in-cluster only** — no public internet Ingress. Callers are internal workloads (Admin, other services) on ClusterIP / internal DNS.

Track items here so we do not re-debate priority every session.  
Status: `open` | `doing` | `done` | `wontfix`

---

## Security / access

### TD-SEC-001 — API key (or equivalent) before write/read from callers
| | |
|--|--|
| **Status** | `open` — **planned, not now** |
| **Why deferred** | Engine is **not** exposed outside the cluster. Trust boundary today = network (ClusterIP + who can reach the Service). |
| **Later intent** | Require a shared **API key** (header) — or mTLS / mesh identity — before any caller can hit product APIs (`/wallets`, `/integrations/webhooks`, `/movements`, `/digestion-rules`, …). |
| **Scope when done** | All mutating routes at minimum; prefer all routes. Admin + internal services send key from env/secret. Reject missing/invalid key with stable 401/403 code. |
| **Not in scope yet** | Public OAuth, end-user JWT, rate limits for internet, SDK auth UX. |
| **Until then** | Do **not** open public Ingress; rely on cluster NetworkPolicy / mesh if available. |
| **Rough design sketch** | `LEDGER_ENGINE_API_KEY` (or multi-key map) in engine env; filter/interceptor on servlet chain; Admin `LEDGER_ENGINE_API_KEY` on BFF proxy. Optional: rotate via Secret without code change. |
| **Exit criteria** | Unauthenticated call from a pod **without** key fails; Admin + documented callers work with key from Secret. |

---

## Platform / ops

### TD-OPS-001 — DB migrations instead of `JPA_DDL_AUTO=create`
| | |
|--|--|
| **Status** | `open` |
| **Note** | Fine for local greenfield/demo. Shared/staging/prod cluster needs Flyway/Liquibase (or equivalent) before multi-instance or durable data. |

### TD-OPS-002 — Cluster network posture (document + verify)
| | |
|--|--|
| **Status** | `open` |
| **Note** | Confirm Service is ClusterIP only; optional NetworkPolicy allow-list (Admin + known producers → engine). Complements TD-SEC-001 until API key lands. |

---

## Product API surface

### TD-API-001 — Legacy `/ledger-*` vs product APIs
| | |
|--|--|
| **Status** | `doing` → controllers `@Deprecated` (still served); Admin legacy pages removed |
| **Note** | Product path = `/wallets`, `/movements`, `/integrations/*`, `/digestion-rules`, `/ingest-policies`, `/coa-profiles`. Legacy: `/ledger-wallets`, `/ledger-accounts`, `/accounts`, `/rules`, `/rule-executions`. Prefer product APIs for new work; remove endpoints only after no in-cluster callers. |

### TD-API-002 — COA segments stay internal
| | |
|--|--|
| **Status** | `done` (policy) |
| **Note** | Account COA segments (`entity`/`type`/…) are **not** on product wallet account DTOs. COA config via `/coa-profiles` + books via `fullNumber` / internal tables. Do not re-expose on public product DTOs. |

---

## Product backlog (not debt, parked)

| ID | Item | Status |
|----|------|--------|
| PB-C | Cap / tier / window (C-lite) | parked until contract needs |
| PB-E | SpEL / heavy rules engine | rejected for now |
| PB-SDK | Client SDK | deferred |
| PB-VANITY | Real vanity code generation | TODO in `WalletVanityCodes` |
| PB-CLAW | Clawback / points expiry cron | backlog |

---

## How to use this file

1. New debt → add ID + status + one-line “why later”.  
2. Starting work → set `doing`, open PR, link this ID in PR body.  
3. Done → `done` + date/PR.  
4. **Do not** implement TD-SEC-001 until Wayne prioritizes it; network boundary is enough for current phase.
- Removed `LedgerMovementOrderTypeConstraintMigrator` — rely on enum column + Flyway later if CHECK needed.
