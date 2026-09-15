# LedgeRX — Glossary & Legend

Plain-English definitions for every key term in [BOOKLET.md](BOOKLET.md) and [SIMPLIFIED.md](SIMPLIFIED.md). Grouped by theme; within a group, alphabetical.

---

## 1 · Product & components

| Term | Meaning |
|---|---|
| **LedgeRX** | The product: a points / multi-currency wallet **system of record**. Tagline: *Earn · burn · books — without redeploy.* |
| **ledger-engine** | The core module (this repo): REST API + rules engine + posting. |
| **ledger-engine-admin-portal** | Separate ops UI for managing wallets, rules, refunds. Talks to the engine over HTTP. |
| **ledger-engine-sdk** | Optional versioned Java 17 client JAR for product backends. Distributed manually, **not** on Maven Central. |
| **System of record** | The single authoritative store of balances. If it's not posted here, the balance didn't happen. |

## 2 · People & entities

| Term | Meaning |
|---|---|
| **ownerId** | The customer identity the ledger stores (CRM `CUST` id). One ownerId → exactly one wallet. Aliases accepted on ingest: `associatedIdentifier`, `userId`. |
| **Wallet** | Container for one `ownerId`. Holds multiple currency accounts. Also exists for the platform itself: the **HOUSE wallet**. |
| **HOUSE** | The platform's own `ownerId`. Its books are the counterparty for every earn/burn. May go negative. Canonical name is `HOUSE` (legacy `PROGRAM` was renamed in place). |
| **CRM** | Upstream customer master. Owns names/contact data — the ledger stores **only** `ownerId` (+ optional `vanityCode`). |
| **mainAccount** | The per-customer digit key inside a `fullNumber` (e.g. `908951901284`). Events carry it so the engine can open the right books. |
| **vanityCode** | Optional human-friendly alias for a wallet. |

## 3 · Accounts & balances

| Term | Meaning |
|---|---|
| **Book / Account** | One currency bucket under a wallet. Member example: `01A31658334-HKD` and `01A31658334-LP`. |
| **fullNumber** | Digit-only account number: `entity(2) + type(2) + subType(2) + mainAccount + buffer(2) + currency(3)`. E.g. `01 01 01 908951901284 00 344` = member HKD book. Uniqueness = all six segments. |
| **Settlement currency** | The "real money" side of a wallet (go-live: **HKD**). Every customer wallet has a settlement twin book plus an LP book. |
| **LP** | Loyalty Points — the points currency. 0 decimal places, rounding DOWN. |
| **Cashback** | Rewards paid in settlement currency (HKD) instead of LP. Set per rule via `resultCurrency`. |
| **ledgerBalance** | The book balance. Moves on EARN/BURN/deposit/withdraw. Holds don't touch it. |
| **availableBalance** | `ledgerBalance` minus holds (and pending AUTH-only earnings). What can actually be spent. |
| **applyTo** | Event field choosing which balance an earn writes: `BOTH` (default) · `LEDGER` only (AUTH/pending) · `AVAILABLE` only (post-AUTH, can never exceed ledger). Reversals copy the original's `applyTo`. |
| **Hold / Release** | Freeze spendable balance without moving the ledger: HOLD ↓ available, RELEASE ↑ available (≤ ledger). REST-only: `/wallets/holds`, `/wallets/releases`. |

## 4 · Events & fields

| Term | Meaning |
|---|---|
| **TransactionalEvent** | The one JSON shape for everything upstream sends (spend, loan, like). Same JSON via webhook REST or Kafka. |
| **eventId** | Upstream's unique id for one occurrence. Feeds idempotency (`movementKey`). |
| **eventType** | The product family token: `CC_TXN` (credit-card spend), `CC_CIP`, `CC_SIP`, `LN_TXN` (loan), or non-financial types like `LIKE_FB_PAGE`. Door, Brain, and Accounting all key off this **same** token. |
| **action** | How to book **this** fire of an event. Omit = `SPEND`. Others: `REFUND`, `VOID`, `CHARGEBACK` (all full reverses), `PARTIAL`, `ADJUST` (recognised, not booked yet → `ACTION_UNSUPPORTED`). Aliases: `ORIGINAL`/`APPLY`/`NORMAL`→`SPEND`, `REVERSE`→`VOID`, `DISPUTE`→`CHARGEBACK`, `ADJUSTMENT`→`ADJUST`. |
| **originalEventId** | Points a reverse action at the event it undoes. Required for REFUND/VOID/CHARGEBACK (missing → `NO_ORIGINAL`). |
| **metadata** | Free-form bag on the event (`mcc`, `channel`, `merchantName`, `useCase`…). Factor rules can match on `metadata.*`; values are stringified (`mcc: 5411` → `"5411"`). |
| **mcc** | Merchant category code (ISO 18245), e.g. `5411` = grocery. Common door gate. |
| **occurredAt** | When it happened upstream. `ageDays` (event age) derives from it — used for staleness gates. |
| **Auto-wallet** | Engine behaviour: first eligible event for a wallet-less customer creates the HKD+LP books in the same TX. On by default after bootstrap; off → `NO_WALLET`. |
| **Envelope unwrapping** | `JSONUtil.readValue` accepts wrapper keys `payload` / `data` / `event` / `body` if the inner object has `eventId`. |

## 5 · Pipeline stages (the flow)

| Term | Meaning |
|---|---|
| **Door** | Stage 1 — *admission*. Backed by **ingest-policies**. Asks: is this event type enabled, do its `entryFactors` match, and should we auto-wallet? Never scores points. |
| **Brain** | Stage 2 — *scoring*. Backed by **digestion-rules**. Asks: which rule matches first, and how many points does its formula yield? |
| **Accounting** | Stage 3 — *booking*. Maps `eventType` → COA profile → segments + currency, walks CR/DR legs. |
| **Posting** | Stage 4 — *execution*. `ApplyPostingUseCase` writes the movement, legs, and balances under lock. |
| **First bingo** | Rule-selection rule: the first matching Brain rule wins (priority ASC, id ASC). No stacking. |
| **IngestTransactionUseCase** | The orchestrator both REST webhook and Kafka listener call. |

## 6 · Rules & factors

| Term | Meaning |
|---|---|
| **Factor** | One matcher leaf: `{ field, op, value }`, e.g. `currency eq HKD`, `amount gte 100`, `metadata.merchantName startsWith MTR`. Shared by Door (`entryFactors`) and Brain (`whenFactors`) via `FactorMatcher`. |
| **FactorSet** | Boolean combinator over factors: `all` (AND), `any` (OR), `atLeast`+`count` (K-of-N), `exactly`/`atMost`, `not`, `oneOf`, `anyGroup`/`allGroups`. |
| **entryFactors** | Door-side conditions to admit an event. |
| **whenFactors** | Brain-side conditions for a rule to fire. Legacy columns (`minAmount`, currency, mcc, age) still AND with these. |
| **Formula** | JSON point-scorer on a Brain rule. Types: `RATE` (amount×rate), `FIXED` (flat), `LINEAR`, `AMOUNT` (1:1), `TIERED_RATE` (marginal brackets), `TABLE` (by metadata key). Optional `multiplier` / `cap` / `floor`. No SpEL. |
| **resultCurrency** | Which book the reward lands in: `LP` (loyalty) or `HKD` (cashback). |
| **eligibilityTrace / matchedPath** | Explainability output: why a rule matched, e.g. `G12 > F1 > currency:eq`. Returned in the webhook response and shown in Admin dry-run. |
| **Priority** | Numeric sort on rules; lower runs first. Ties broken by id ASC. |
| **Dry-run** | `POST /integrations/webhooks/transactions/dry-run` — evaluates Door+Brain without booking. |

## 7 · Money movements

| Term | Meaning |
|---|---|
| **Movement** | One booked transaction (`ledger_movement`). Has `orderType` (EARN, BURN…), `status`, `movementKey`. |
| **Leg** | One side of a movement (`ledger_entry`). Minimum 2 per movement: one DEBIT, one CREDIT, same amount, same currency. Stored as positive amounts + `MovementDirection`. |
| **EARN** | Give the member points/cashback. Member ↑, HOUSE operating book is counterparty. |
| **BURN** | Member spends/redeems points. Member ↓, HOUSE counterparty. |
| **DEPOSIT / WITHDRAWAL** | Money rails — single-sided on the member book. |
| **IN_WALLET_TRANSFER** | Move between two books of the same wallet (e.g. HKD→LP convert). |
| **ADJUSTMENT_REFUND** | The posting type behind every refund/void/chargeback: original legs reversed, DR/CR swapped, same books. |
| **Refund · Void · Chargeback** | Three `action` flavours of full reverse. Refund = customer return; Void = same-day, never captured; Chargeback = issuer dispute. All idempotent against each other. |
| **PARTIAL / ADJUST** | Recognised reverse/modify intents **not booked yet** — rejected with `ACTION_UNSUPPORTED`. |
| **MovementKey** | Idempotency key per movement, e.g. `loyalty-earn-{eventId}`, `{originalKey}-refund`. A repeat returns the existing row, not a new one. |
| **SETTLED / REFUNDED** | Movement statuses. New movements are SETTLED; fully reversed originals flip to REFUNDED. |

## 8 · Accounting & COA

| Term | Meaning |
|---|---|
| **Double-entry (DE)** | Every movement has ≥2 balanced legs: total DEBIT = total CREDIT, same currency both sides. |
| **DR / CR** | Debit / Credit. T-account: DR left, CR right. **Loyalty convention: CREDIT = add to member, DEBIT = subtract.** |
| **COA** | Chart of accounts — internal Finance structure. **Not a public API concern**: product events carry only `eventType`, never COA segments. |
| **COA profile** | Maps an `eventType` (default: profile `code` ≡ `transactionCode` ≡ `eventType`) to number segments + points currency. APIs: `/coa-profiles`, `/coa-dictionary`, `/corporate-coa`. |
| **Custodian (01-01-01)** | The member's own books (entity 01, type 01, subType 01). |
| **House operating (01-02-01)** | HOUSE's book, mainAccount 9999. The counterparty leg of every earn/burn. May go negative. |
| **House expense (01-04-02)** | Finance's expense book for loyalty liability. |
| **AccountingRuleExecution** | The walk that turns a COA profile into concrete CR/DR legs. |
| **Recipe** | Named composition of atoms for UA-sheet use cases: `eventType|metadata.useCase` → `PostingRecipeCatalog` → posting. E.g. `CC_TXN_HKD_TO_LP` = earn HKD + convert 1:1 to LP. |
| **Atom** | Building block of a recipe: `CREDIT_REWARD` (earn), `REDEEM` (burn), `CASHBACK` (burn, payout rail later), `CONVERT_HKD_TO_LP`. |

## 9 · Outcomes & failure codes

| Code | Meaning |
|---|---|
| **`NOT_ENTERED`** | Door rejected it (MCC / currency / amount / age gate). |
| **`NO_WALLET`** | No wallet exists and auto-wallet is off. |
| **`NO_RULE` / `SKIPPED`** | Brain had no matching rule (no bingo). |
| **`DUPLICATE`** | Same `eventId` already booked (`movementKey` hit) — idempotent no-op. |
| **`ACTION_UNSUPPORTED`** | `PARTIAL` or `ADJUST` — recognised shape, not booked yet. |
| **`NO_ORIGINAL`** | Reverse action without `originalEventId`. |
| **`ACTION_UNSUPPORTED` vs error** | These are *outcomes* on the response (`status`), not exceptions. Hard failures raise `BizException` + domain `*ErrorResponse`. |
| **Fail queue** | `/integrations/failed-transactions` — dead events land here; replay via `POST /integrations/failed-transactions/{id}/replay`. |
| **`WAL0409`** | Error when onboarding a duplicate `ownerId`. |

## 10 · Tiering

| Term | Meaning |
|---|---|
| **Wallet tier** | Band (e.g. Silver/Gold) written on the wallet after each settled LP movement, in the same TX. |
| **Tier policy** | Door-shaped config at `/wallet-tier-policies`. Criterion v1 = **`LEDGER_BALANCE`** (sum of that wallet+currency ledger balances; default LP). |
| **upgradeAt / downgradeBelow** | Band thresholds. Upgrade: balance ≥ `upgradeAt`. Downgrade: balance < `downgradeBelow` (blank → reuse `upgradeAt`). |
| **Tier events** | Outbound: `LEDGER_BALANCE_UPDATED` always; `WALLET_TIER_CHANGED` only when the band moved. HOUSE skipped; holds cause no tier change. |

## 11 · Integration & ops

| Term | Meaning |
|---|---|
| **Webhook** | `POST /integrations/webhooks/transactions` — the REST ingest door. |
| **Kafka ingest** | Alternative ingest: topic `ledger.transaction.events`, key `eventId`, group `ledger-engine`. Off by default (`LEDGER_KAFKA_ENABLED=true`). |
| **Movement Kafka (outbound)** | After each settled movement: `ledger.balance.updated` (key `walletId`); also `ledger.movement.done`. Needs `LEDGER_MOVEMENT_KAFKA_ENABLED=true`. |
| **SDK handshake** | `GET /integrations/sdk-info` → engineVersion, minSdkVersion, features. Catalog: `/integrations/use-cases`. |
| **Simulator** | `simulator/` — generic CRM/integrator load tool. Modes: `backfill`, `webhook`, `kafka`, `both`. Re-runs are safe. |
| **bootstrap-runtime.sh** | Seeds default Door + Brain policies. Go-live step 1. |
| **Batch onboarding** | `POST /wallets/batch` — soft-idempotent (`alreadyExists`), max 1000 per chunk. Production backfill = export CRM ids → batch to staging → production. |
| **Phase 2** | Turning on real upstream traffic (POS/OMS → webhook/Kafka) after wallets are backfilled and counts match. |
| **Result / R.success** | The API response envelope: `data` + `requestId`. |
| **BizException** | The engine's error type; each domain has a matching `*ErrorResponse`. |
| **Admin flow strip** | The portal's visual pipeline: Door → Brain → Accounting → Ledger. |
| **DTO / DtoWrapper** | `Create*RequestDto` / `Get*ResponseDto` types; `DtoWrapper` is the **only** allowed PO→DTO mapper. |
| **Money-as-string** | Convention: amounts are currency-scaled strings (`"100.00"` HKD, `"5"` LP), never floats. |

## 12 · Frozen, parked, removed

| Term | Meaning |
|---|---|
| **Feature freeze (core)** | Door/Brain factors, posting, recipes, hold, money rails, Admin path, this booklet — stable, don't churn. |
| **Parked** | Deliberately deferred: true Expense-GL pool, cashback payout rail, rule stacking, named factor packs, MTD counters, PARTIAL/ADJUST booking. |
| **Out of scope** | Never in this engine: payment gateway/card rails, CRM master, compliance UI, multi-party settlement orchestration. |
| **Removed** | `AccountSet` / sub-accounts (gone), legacy APIs `/ledger-wallets` · `/ledger-accounts` · `/accounts` (removed, TD-API-001). |
| **Open debt** | TD-SEC-001 (API key), TD-API-001 (legacy API removal). TD-OPS-001 (migrations) done — Liquibase. |

---

*Cross-references: architecture & flow → [SIMPLIFIED.md](SIMPLIFIED.md) · full detail → [BOOKLET.md](BOOKLET.md).*
