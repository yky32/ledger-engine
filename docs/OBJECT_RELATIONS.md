# LedgeRX — Object Relations

How the engine's objects relate: the persisted entity model (ER), then the runtime collaboration (who calls whom during one event). Entity fields below are taken from the actual `entity/po/*` classes in `src/main/java`.

> Companion docs: [BOOKLET.md](BOOKLET.md) (source of truth) · [SIMPLIFIED.md](SIMPLIFIED.md) (requirements & flow) · [GLOSSARY.md](GLOSSARY.md) (terms).

---

## 1 · Entity-relationship map

```mermaid
erDiagram
    WALLET ||--o{ ACCOUNT : "walletId — books per currency"
    WALLET ||--o{ LEDGER_MOVEMENT : "walletId — owner of every movement"
    LEDGER_MOVEMENT ||--o{ LEDGER_ENTRY : "txnId — the double-entry legs"
    LEDGER_MOVEMENT |o--o| LEDGER_MOVEMENT : "associatedLedgerMovementId — refund↔original"

    INGEST_POLICY ||..o{ WALLET : "auto-wallet creates (HKD+LP) in-TX"
    DIGESTION_RULE }o..|| LEDGER_MOVEMENT : "scores the event that becomes a movement"
    COA_PROFILE }o..|| LEDGER_MOVEMENT : "eventType → segments → legs"
    ACCOUNTING_RULE_EXECUTION ||--o{ ACCOUNTING_RULE : "walk → CR/DR atoms"
    WALLET_TIER_POLICY ||..o{ WALLET : "assessed per settled LP movement"

    COA_PROFILE {
        string code "≡ eventType (default)"
        string transactionCode "optional override"
        string entity
        string type
        string subType
        string buffer
        string currency "points book, default LP"
        long   walletId "pool / house wallet"
    }
    WALLET {
        long   id PK
        string ownerId "UNIQUE — 1 wallet per customer"
        string settlementCurrency "HKD"
        string tier "written in-TX by tiering"
        string vanityCode
        string status
    }
    ACCOUNT {
        long   id PK
        long   walletId FK
        string fullNumber "UNIQUE 2+2+2+main+2+3"
        string mainAccount "client identifier"
        string currency
        decimal ledgerBalance
        decimal availableBalance
        boolean allowNegative
    }
    LEDGER_MOVEMENT {
        long   id PK
        long   walletId FK
        string movementKey "UNIQUE — idempotency"
        string orderType "EARN·BURN·DEPOSIT…"
        string status "SETTLED·REFUNDED"
        string applyTo "BOTH·LEDGER·AVAILABLE"
        long   associatedLedgerMovementId "self-FK for reversals"
    }
    LEDGER_ENTRY {
        long   id PK
        long   txnId "FK = movement.id"
        string targetId
        decimal amount "always positive"
        string direction "DEBIT·CREDIT"
        boolean affectsLedger
        boolean affectsAvailable
    }
```

*(Dashed lines `..>` = logical/config-driven links resolved at runtime; solid `--` = persisted FK columns.)*

---

## 2 · The same map, grouped by role

```text
CONFIGURATION (ops-managed, "no redeploy")      TRANSACTIONAL (written by the engine)
┌───────────────────────────────┐               ┌──────────────────────────────────┐
│ IngestPolicy   (the Door)     │               │ Wallet  ──1:N──▶ Account         │
│   entryFactors                │               │   (balances live on Account)     │
│   isAutoCreateWallet ─────────┼──────────▶    │ Wallet.tier ◀── WalletTierPolicy │
│ DigestionRule  (the Brain)    │  admit &      │                                  │
│   whenFactors + formula       │  score        │ LedgerMovement ──1:N──▶ LedgerEntry
│   eventType, priority         │               │   movementKey (idempotency)      │
│ CoaProfile (the booking map)  │               │   associatedLedgerMovementId     │
│   code ≡ eventType            │               │     (refund → original)          │
│ CoaDictionary (finance terms) │               │ FailedTransactionIngest (dead    │
│ AccountingRuleExecution       │               │   letter queue: rawPayload,      │
│   └─▶ AccountingRule atoms    │               │   failureCode, replay)           │
└───────────────────────────────┘               └──────────────────────────────────┘
```

Config rows are **read** during ingest; transactional rows are **written** by posting. The two halves meet at `eventType`: policy, rule, profile, and event all carry the same token.

---

## 3 · Runtime collaboration — one CC_TXN event

```mermaid
flowchart TB
    EVT["TransactionalEvent<br/>(eventId, ownerId, eventType, amount, metadata)"]

    subgraph INGEST["IngestTransactionUseCase"]
        DOOR["Door — IngestPolicy<br/>entryFactors match?"]
        AUTOW["auto-wallet<br/>creates Wallet + HKD/LP Accounts"]
        BRAIN["Brain — DigestionRule<br/>first bingo (priority ASC)<br/>formula → points + resultCurrency"]
        COA["CoaProfile<br/>eventType → segments + currency"]
        WALK["AccountingRuleExecution<br/>→ AccountingRule atoms<br/>DR/CR plan"]
    end

    subgraph POSTING["Posting (single write path)"]
        APUC["ApplyPostingUseCase"]
        MV["LedgerMovement<br/>movementKey = loyalty-earn-{eventId}"]
        LE["LedgerEntry × 2<br/>DR + CR, same currency"]
        ACC["Account.ledgerBalance / availableBalance<br/>(row-locked)"]
    end

    TIER["AssessWalletTierUseCase<br/>Wallet.tier ← WalletTierPolicy"]
    KAFKA["outbound: ledger.balance.updated<br/>LEDGER_BALANCE_UPDATED / WALLET_TIER_CHANGED"]
    FAIL["FailedTransactionIngest<br/>(on NO_WALLET / NOT_ENTERED / hard error)"]

    EVT --> DOOR
    DOOR -- "no wallet & isAutoCreateWallet" --> AUTOW
    DOOR -- "entered" --> BRAIN
    BRAIN -- "bingo" --> COA --> WALK --> APUC
    APUC --> MV --> LE --> ACC
    ACC -. "same TX" .-> TIER
    APUC -. "after settle" .-> KAFKA
    DOOR -- "rejected / no wallet" --> FAIL

    REFUND["REFUND / VOID / CHARGEBACK event<br/>or POST /movements/{id}/refund"] -. "intercepts before Door;<br/>skips Door & Brain" .-> APUC
    MV -. "ADJUSTMENT_REFUND:<br/>DR/CR swapped, original→REFUNDED" .-> REFUND
```

---

## 4 · Relationship reference

| Relationship | Cardinality | Join / mechanism | Notes |
|---|---|---|---|
| Wallet → Account | **1 : N** | `account.walletId` | HKD + LP books minimum; balances live here |
| Wallet → LedgerMovement | **1 : N** | `movement.walletId` | Every movement belongs to one wallet |
| LedgerMovement → LedgerEntry | **1 : N (min 2)** | `entry.txnId = movement.id` | Legs: one DEBIT + one CREDIT |
| LedgerMovement → LedgerMovement | **0..1 : 0..1** | `associatedLedgerMovementId` (self-FK) | Reverse row points at original; original flips `REFUNDED` |
| Event → Movement | **N : 1** | `movementKey = loyalty-earn-{eventId}` | Idempotency: repeat `eventId` → `DUPLICATE`, existing row returned |
| IngestPolicy ⇢ Wallet | config → data | auto-wallet, same TX | Off → `NO_WALLET` |
| DigestionRule → outcome | config → data | `eventType` + `priority` + `whenFactors` | First bingo; no stacking |
| CoaProfile → legs | config → data | `code` (≡ `eventType`) → segments + `currency` | Also `walletId` → pool/house wallet (`HOUSE`, may go negative) |
| AccountingRuleExecution → AccountingRule | 1 : N | the walk | Direction, multiplier, target account per atom |
| WalletTierPolicy ⇢ Wallet.tier | config → data | assessed per settled LP movement, same TX | `LEDGER_BALANCE` criterion; upgradeAt/downgradeBelow bands |
| Movement → outbound events | 1 : N (async) | Kafka after settle | `ledger.balance.updated`, `WALLET_TIER_CHANGED` |
| Failed ingest → replay | 1 : 1 | `/integrations/failed-transactions/{id}/replay` | Stores `rawPayload` + `failureCode` |

---

## 5 · Rules the diagram encodes (read this before changing relations)

1. **One `ownerId` → one Wallet** — enforced unique; `WAL0409` on duplicate onboarding.
2. **Balances live only on Account**, mutated under lock, and **only** via `ApplyPostingUseCase`. Any other write path = drift.
3. **Movement ↔ legs invariant**: Σ DEBIT = Σ CREDIT, same currency, amounts stored positive + `MovementDirection`.
4. **Idempotency is a unique key**, not a check-then-insert: `movementKey` dedupes earns/burns; `{originalKey}-refund` dedupes all three reverse actions.
5. **Refunds are movements too** — a new `ADJUSTMENT_REFUND` movement linked back via `associatedLedgerMovementId`, never an update to the original (except status → `REFUNDED`).
6. **`applyTo` rides on the movement** (`BOTH`/`LEDGER`/`AVAILABLE`); entries carry `affectsLedger` / `affectsAvailable` flags derived from it.
7. **Config is data**: Door, Brain, COA, tiering are rows in Postgres — changing scoring needs no deploy, and entries in this doc's left column never hard-reference a wallet or customer.
8. **Tiering writes in the posting TX** (`wallet.tier`), not via a later Kafka consumer — the Kafka events are announcements, not the source of tier truth.

---

*Source of entity fields: `src/main/java/com/altech/ledger/entity/po/**` (FK verified in `LedgerMovementExecutionUseCase`). Diagrams are mermaid — render on GitHub or any mermaid-capable viewer.*
