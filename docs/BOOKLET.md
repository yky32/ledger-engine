# LedgeRX — Product & Engineering Booklet

**Single source of truth.**  
Module: `ledger-engine` · Admin: `ledger-engine-admin-portal` · Optional SDK: `ledger-engine-sdk`  
Deploy: **in-cluster only** (API key = later tech debt)

| | |
|--|--|
| **Product** | **LedgeRX** |
| **Tagline** | Earn · burn · books — without redeploy |
| **Chinese** | LedgeRX 積分賬本 |

---

## Table of contents

1. [What it is](#1-what-it-is)
2. [Core model](#2-core-model)
3. [Pipeline](#3-pipeline-door--brain--books)
4. [Posting engine](#4-posting-engine)
5. [Factors](#5-factors-door--brain)
6. [Equations](#6-equations-brain)
7. [Hold / available vs ledger](#7-hold--available-vs-ledger)
8. [COA](#8-coa-profile)
9. [API surface](#9-api-surface)
10. [Webhook contract](#10-webhook-contract-sketch)
11. [Bootstrap & local](#11-bootstrap--local)
12. [Admin map](#12-admin-map)
13. [UAF notes](#13-uaf-notes)
14. [Feature freeze & debt](#14-feature-freeze--tech-debt)
15. [Decks](#15-decks)
16. [Use-case recipes (UA mapping)](#16-use-case-recipes-ua-mapping)

---

## 1. What it is

LedgeRX is the **points / multi-currency wallet system of record**:

- Upstream tells **what happened** (purchase, redeem, …)
- **Door** decides whether the event is admitted
- **Brain** decides eligibility + how many points
- **Books** post true double-entry (loyalty) or single-sided money rails
- Ops change rules **without redeploy** (DB config + Admin)

```text
LedgeRX
├── Core        ledger-engine
├── Admin       ledger-engine-admin-portal
└── (optional)  ledger-engine-sdk
```

---

## 2. Core model

| Rule | |
|------|--|
| **1 `ownerId` → 1 Wallet** | Multi-ccy = accounts under the wallet |
| **No AccountSet** | Cancelled product concept |
| **COA** | Opens book structure; **does not** score points |
| **Standalone** | No hardcoded UAF seed in engine |
| **Identity** | Full `ownerId`; card vs loan = product-level ownerIds or COA streams |

```text
Wallet (ownerId)
  ├─ HKD account     ← deposit / withdraw
  └─ LP account      ← earn / burn / hold
```

---

## 3. Pipeline (Door → Brain → Books)

```text
Event
  → Door  (isEnabled + entryFactors)     → NOT_ENTERED if reject
  → Brain (whenFactors ∧ legacy + formula[+mult/cap])
  → first rule by priority
  → ApplyPostingUseCase (EARN / BURN)
  → PROGRAM DE legs + balances / movements
```

### Door vs Brain

| | **Door (`/ingest-policies`)** | **Brain (`/digestion-rules`)** |
|--|-------------------------------|--------------------------------|
| Question | Accept traffic? Auto-wallet? | Eligible? How many points? |
| Cardinality | ~1 policy | N rules |
| Factors | `entryFactors` | `whenFactors` |
| Scoring | **Never** | Formula JSON |
| Legacy filters | — | minAmount · ccy · mcc · age AND-compiled with whenFactors |

**Do not** put currency/MCC/age scoring gates only on Door.  
**Do not** merge Door + Brain into one table.

---

## 4. Posting engine

All balance writes go through:

```text
PostingCommand + PostingIntent
        → ApplyPostingUseCase.execute(...)
        → LedgerMovementShooter
        → Execution by OrderType
```

| Intent | Member book | Counterparty |
|--------|-------------|--------------|
| **DEPOSIT** | + | Single-sided (funding) |
| **WITHDRAWAL** | − | Single-sided |
| **IN_WALLET_TRANSFER** | ± | Other wallet |
| **EARN** | + | **PROGRAM pool DE** |
| **BURN** | − | **PROGRAM pool DE** |
| **HOLD / RELEASE** | available only | ledger unchanged |

### Code

```java
applyPostingUseCase.execute(PostingCommand.deposit(...));
applyPostingUseCase.execute(PostingCommand.earn(...));
// convenience
applyPostingUseCase.earn(walletId, points, LP, key, desc);
applyPostingUseCase.burn(...);
```

### Do not

| ❌ | Why |
|----|-----|
| Earn via deposit API | No PROGRAM legs → false mint |
| Burn via withdrawal API | No PROGRAM reclaim |
| New balance code bypassing `ApplyPostingUseCase` | Drift |

### Loyalty double-entry

```text
EARN N LP   DR PROGRAM.LP N   CR member.LP N
BURN N LP   DR member.LP N    CR PROGRAM.LP N
```

- PROGRAM owner id: `PROGRAM` (lazy on first earn/burn)
- Pool: `allowNegative=true`

Product money rails stay single-sided unless a future nostro design expands them.

---

## 5. Factors (Door + Brain)

Shared leaf + FactorSet matcher.

### Leaf

```json
{ "id": "F1", "field": "currency", "op": "eq", "value": "HKD" }
{ "field": "mcc", "op": "in", "value": ["5411"] }
{ "field": "amount", "op": "gte", "value": 100 }
{ "field": "ageDays", "op": "lte", "value": 30 }
{ "field": "metadata.channel", "op": "eq", "value": "POS" }
```

| field | |
|-------|--|
| `currency` / `ccy` | event ISO |
| `mcc` | metadata |
| `amount` | |
| `ageDays` | from `occurredAt` |
| `eventType` | |
| `metadata.*` | |

**Ops:** `eq` · `neq` · `in` · `nin` · `gt/gte/lt/lte` · `between` · `exists`

### FactorSet (boolean)

Plain **array** = AND all (compat).

| match | UAF play |
|-------|----------|
| `any` | 1 of N |
| `atLeast` + `count` | K of N |
| `exactly` / `atMost` | precise counts |
| `not` | exclude pack |
| `oneOf` | exclusive |
| `anyGroup` / `allGroups` | (A∧B)∨(C∧D) |

```json
{ "match": "any", "factors": [F1, F2, F3, F4, F5] }

{ "match": "atLeast", "count": 2, "factors": [F1, F2, F3, F4, F5] }

{
  "match": "anyGroup",
  "groups": [
    { "id": "G12", "factors": [F1, F2] },
    { "id": "G34", "factors": [F3, F4] }
  ]
}
```

### Explain

`eligibilityTrace[]`: `ruleCode` · `matched` · `failStep` · `detail` · **`matchedPath`**  
Example path: `G12 > F1 > currency:eq`

### Parked (not blocking)

Named packs · multi-rule stacking · lifetime counters · governance versions

---

## 6. Equations (Brain)

Formula JSON only — **no SpEL**.

| type | |
|------|--|
| `RATE` | amount × rate |
| `FIXED` | constant |
| `LINEAR` | rate×amount + fixed |
| `AMOUNT` | points = amount |
| `TIERED_RATE` | marginal brackets |
| `TABLE` | by metadata key (e.g. tier) |

Extras on any type: **`multiplier`** · **`cap`** · **`floor`**

```json
{ "type": "RATE", "rate": 0.01, "cap": 50, "floor": 1, "multiplier": 2 }

{
  "type": "TIERED_RATE",
  "brackets": [
    { "upTo": 5000, "rate": 0.01 },
    { "upTo": null, "rate": 0.02 }
  ]
}

{
  "type": "TABLE",
  "by": "tier",
  "map": {
    "GOLD": { "type": "RATE", "rate": 0.02 },
    "DEFAULT": { "type": "RATE", "rate": 0.01 }
  }
}
```

Demo baseline: 500 HKD × 1% = **5 LP** (MCC 5411 grocery).

---

## 7. Hold / available vs ledger

| | ledger | available |
|--|--------|-----------|
| EARN | ↑ | ↑ together |
| HOLD | unchanged | ↓ |
| RELEASE | unchanged | ↑ (≤ ledger) |
| BURN | ↓ | ↓ |

Earn **cannot** credit available-only without hold path — use **HOLD** for A ≠ L.

```bash
POST /wallets/holds
POST /wallets/releases
```

Idempotent by `movementKey`. Entries: `affectsLedger=false` for hold/release (as-of ledger ignores; available still moves).

---

## 8. COA profile

| | |
|--|--|
| Table | `coa_profile` (flat) |
| API | `/coa-profiles` |
| Onboard | `POST /wallets` optional `coaProfileCode` → DEFAULT |

Segments (`entity` / `type` / …) are **internal**. Not on product account DTOs.

Door lazy wallet: `metadata.coaProfileCode` → policy `autoWalletCoaProfileCode` → DEFAULT.

---

## 9. API surface

### Product (use these)

| Area | Path |
|------|------|
| Wallet | `/wallets` |
| Money | `/movements/deposits` · `/withdrawals` · `/transfers/in-wallet` · settle |
| Hold | `/wallets/holds` · `/releases` |
| History | `/wallets/{ownerId}/movements` · balances as-of |
| Door | `/ingest-policies` |
| Brain | `/digestion-rules` |
| Webhook | `/integrations/webhooks/transactions` · `/dry-run` |
| Fail queue | `/integrations/failed-transactions` |
| Legs | `/integrations/ledger-entries?movementId=` or `eventId=` |
| COA | `/coa-profiles` |
| Config | `/configurations` |

**No `/by-XXXX` paths.** Pagination 1-based where applicable.

### Legacy (deprecated, still served)

`/ledger-wallets` · `/ledger-accounts` · `/rules` · … → prefer product paths above.

### Money use cases

| UseCase | API | Intent |
|---------|-----|--------|
| `CreateDepositUseCase` | `POST /movements/deposits` | DEPOSIT |
| `CreateWithdrawalUseCase` | `POST /movements/withdrawals` | WITHDRAWAL |
| `CreateInWalletTransferUseCase` | `POST /movements/transfers/in-wallet` | IN_WALLET_TRANSFER |
| (+ settle / query) | | |

Loyalty: webhook → Brain → `ApplyPostingUseCase` EARN/BURN — **not** deposit/withdraw APIs.

---

## 10. Webhook contract (sketch)

```http
POST /integrations/webhooks/transactions
POST /integrations/webhooks/transactions/dry-run
```

Body (conceptual): `eventId` · `ownerId` · `eventType` · `amount` · `currency` · `occurredAt` · `metadata` (mcc, channel, tier, …)

Response highlights:

- `status` · `points` · `matchedRuleCode` · `movementId` · `legs`
- `eligibilityTrace[]` with **`matchedPath`**

Idempotency: `loyalty-{op}-{eventId}` movement keys.

---

## 11. Bootstrap & local

```bash
# Postgres often localhost:5433
mvn spring-boot:run
# JPA_DDL_AUTO=create  → fresh schema each boot (pre-UAT OK)

./scripts/bootstrap-runtime.sh
./scripts/e2e-smoke.sh
./scripts/upstream-sim.sh
```

Bootstrap (idempotent): Door defaults + sample Brain rules (e.g. PURCHASE 1%, SIGNUP fixed, REDEEM burn).

Admin: `LEDGER_ENGINE_URL=http://localhost:8080`

---

## 12. Admin map

| Screen | |
|--------|--|
| Door | entryFactors + FactorSet presets |
| Brain | whenFactors presets + formula builder (tier/table/cap) |
| Demo | guided earn path |
| Webhook / simulator | dry-run + **path** column |
| COA · wallets · holds · review · records | ops |

---

## 13. UAF notes

Pitch: **1 LedgeRX + SDK**, not N ledger microservices.

- Stress factors: any / K-of-N / combos → FactorSet
- Event catalogue (~21 CC + loan): workshop skeleton in decks; **config not hardcoded in engine**
- Context (MTD, tier lifetime): inject as **metadata / wallet attributes**, engine evaluates — not customer-360 calculator
- Stacking multiple bonuses: explicit product switch later — default first-match by priority

---

## 14. Feature freeze & tech debt

### Feature freeze (core)

Door/Brain factors · posting · hold · money rails · DE earn/burn · Admin path · docs/decks  
→ **enough to demo and sell architecture.** New capability needs explicit ask.

### Open debt

| ID | Item | Status |
|----|------|--------|
| TD-SEC-001 | API key before product APIs | open (cluster trust OK now) |
| TD-OPS-001 | Flyway vs ddl-create | open |
| TD-OPS-002 | NetworkPolicy posture | open |
| TD-API-001 | Remove legacy `/ledger-*` when callers gone | doing / deprecated |

### Parked product

Stacking · named factor catalog · counters · clawback cron · C-pack referrer — backlog.

---

## 15. Decks

| File | |
|------|--|
| `docs/decks/LedgeRX-UAFinance-Full-Briefing.pptx` | Blessed landscape + appendix |
| `docs/decks/LedgeRX-Factor-Playbook.pptx` | Factors / boolean / equation |
| `docs/decks/LedgeRX-UAF-Earn-Process-Burn-Sequence.pptx` | Earn→process→burn walkthrough |
| Sequence HTML/PNG under `docs/decks/` | |

---

## Quick curl map

```bash
# Door
curl -sS localhost:8080/ingest-policies
curl -sS -X PUT localhost:8080/ingest-policies -H 'Content-Type: application/json' -d '{...}'

# Brain
curl -sS localhost:8080/digestion-rules
curl -sS -X POST localhost:8080/digestion-rules -H 'Content-Type: application/json' -d '{...}'

# Wallet
curl -sS -X POST localhost:8080/wallets -H 'Content-Type: application/json' -d '{"ownerId":"CUST-1","settlementCurrency":"HKD","accounts":[{"currency":"LP"}]}'

# Money
curl -sS -X POST localhost:8080/movements/deposits -H 'Content-Type: application/json' -d '{"ownerId":"CUST-1","currency":"HKD","amount":100}'

# Loyalty
curl -sS -X POST localhost:8080/integrations/webhooks/transactions/dry-run -H 'Content-Type: application/json' -d '{...}'
curl -sS -X POST localhost:8080/integrations/webhooks/transactions -H 'Content-Type: application/json' -d '{...}'

# Hold
curl -sS -X POST localhost:8080/wallets/holds -H 'Content-Type: application/json' -d '{"ownerId":"CUST-1","currency":"LP","amount":3,"movementKey":"h1"}'

# Legs
curl -sS 'localhost:8080/integrations/ledger-entries?movementId=123'
```

---

*Booklet supersedes the previous multi-file docs split. Historical copies live under `docs/archive/` if needed.*

---

## 16. Use-case recipes (UA mapping)

COA segments are **internal Finance language**. Product / upstream uses **recipe codes** only.

```text
eventType (or metadata.useCase)
    → PostingRecipeCatalog
    → atoms[] → ApplyPostingRecipeUseCase
    → ApplyPostingUseCase (EARN/BURN legs)
```

### Codes (CC)

| code | atoms |
|------|--------|
| `CC_TXN_HKD` | CREDIT_REWARD @ HKD |
| `CC_TXN_LP` | CREDIT_REWARD @ LP |
| `CC_TXN_HKD_REDEEM` | CREDIT + REDEEM |
| `CC_TXN_HKD_CASHBACK` | CREDIT + CASHBACK |
| `CC_TXN_LP_REDEEM` | CREDIT + REDEEM @ LP |
| `CC_TXN_LP_CASHBACK` | CREDIT + CASHBACK @ LP |
| `CC_TXN_HKD_TO_LP` | CREDIT HKD + CONVERT→LP |
| `CC_TXN_HKD_LP_REDEEM` | CREDIT + CONVERT + REDEEM |
| `CC_TXN_HKD_LP_CASHBACK` | CREDIT + CONVERT + CASHBACK |

### Codes (Loan / Load alias)

`LOAN_DD_*` same patterns; `LOAD_DD_*` aliases to loan.

### Atoms

| Atom | Books (phase-1) |
|------|-----------------|
| CREDIT_REWARD | EARN (PROGRAM ↔ member) — GL may label pool as Expense |
| REDEEM / CASHBACK | BURN |
| CONVERT_HKD_TO_LP | BURN HKD + EARN LP (1:1) |

### Webhook

```json
{
  "eventId": "txn-1",
  "ownerId": "01A1",
  "eventType": "CC_TXN_LP",
  "amount": 100,
  "currency": "HKD",
  "occurredAt": "2026-08-20T12:00:00Z"
}
```

Brain rule `eventType` should match the code (or use broad rule + `metadata.useCase`).  
Points still come from Brain **formula**; recipe chooses **which books / chain**.

### Wallet map

| Sheet | LedgeRX |
|-------|---------|
| W_1 / owner 01A1 | `ownerId=01A1` |
| W_2 / owner 01A2 | `ownerId=01A2` |

1 ownerId : 1 wallet — aligned.
