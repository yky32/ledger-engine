# LedgeRX

**Product:** **LedgeRX** · **Engine module:** `ledger-engine` `1.0.0`  
Brand note: [docs/BOOKLET.md](docs/BOOKLET.md).

Standalone wallet + ledger core for loyalty & programme books.  
Optional client: [ledger-engine-sdk](https://github.com/yky32/ledger-engine-sdk) (manual JAR delivery; see [DELIVERY](https://github.com/yky32/ledger-engine-sdk/blob/main/docs/DELIVERY.md)).

Java 17 / Spring Boot service for **customer wallets**, **chart-of-accounts balances**, and **movements** (deposit, withdrawal, transfer, earn/burn). It is **ledger core**, not payment rails, identity/CRM, compliance UI, or settlement orchestration.

Product depth and integration flows: **[PRODUCT.md](PRODUCT.md)**, **[INTEGRATION.md](INTEGRATION.md)**.

## Model (strong play)

```text
Customer (CRM id only)     →  ownerId on wallet
        │
     Wallet                →  1 customer : 1 wallet (`settlementCurrency` on row)
        │
     Account set           →  primary (settlement ccy) + optional extra currency lines (e.g. LP)
        │
     LedgerMovement        →  business ops (idempotent movementKey)
        └── LedgerEntry    →  applied credit/debit legs
```

- **Balances** live on `account` (`ledger_balance`, `available_balance`); movements update them under lock.
- **Wallet.settlementCurrency** = default settlement currency (primary account).
- **Unique wallet:** `(owner_id)` — 1 CUST : 1 Wallet.
- Extra books (e.g. LP) are **accounts under the same wallet**, not extra wallet rows.
- **Currency** enum (`fiat` / `LOYALTY_POINT` / `crypto`): `USD`, `HKD`, `LP`, `BTC`, `USDT`, …
- **No classic journal_transaction layer** in the product path; ops are movement + account balances.
- **No startup program-pool seed** — create accounts via API when needed.

## Stack

| | |
|---|---|
| Java / Boot | 17 / 3.5.x |
| DB | PostgreSQL (default host `localhost:5433`, DB `ledger-engine`) |
| Schema | JPA `ddl-auto` (default `create`); Liquibase off (placeholder only) |
| API envelope | `R.success` / `Result` (`com.altech.core`) |
| Errors | `BizException` + domain `*ErrorResponse` |
| Config | `${ENV_VAR:default}` in `application.yml` |

## Build and run

```bash
# DB (example Docker test-db on 5433)
# docker exec test-db psql -U postgres -c 'CREATE DATABASE "ledger-engine";'
# docker exec test-db psql -U postgres -c 'CREATE DATABASE "ledger-engine-test";'

mvn clean package
mvn spring-boot:run
# or: java -jar target/ledger-engine-1.0.0.jar
```

Default: `http://localhost:8080` · health ` /actuator/health` · OpenAPI if springdoc is on path.

```bash
mvn test
```

Docker:

```bash
docker compose up --build
# optional simulator profile — see INTEGRATION.md
cp .env.example .env
docker compose --profile simulator up --build
```

Override DB:

```bash
DB_URL=jdbc:postgresql://host:5432/ledger-engine \
DB_USERNAME=postgres DB_PASSWORD=postgres \
mvn spring-boot:run
```

## Project layout

```text
src/main/java/
├── com.altech.core/          # foundation: R/Result, BizException, AuditEntity,
│                             # Currency/CurrencyType, BaseEvent, utils
└── com.altech.ledger/
    ├── App.java              # Spring Boot entry (no pool seed)
    ├── config/               # Integration, JPA auditing, Kafka props
    ├── endpoint/             # REST (*Endpoint)
    ├── usecase/              # VerbUseCase.execute + CommonUseCase + private _helpers
    ├── entity/
    │   ├── po/               # Account, Wallet, LedgerMovement, LedgerEntry, …
    │   ├── dto/request|response/  # Create*RequestDto, Get*ResponseDto
    │   └── enu/
    ├── repository/
    ├── service/              # MovementBus, DtoMapper, DtoWrapper
    └── listener/             # optional Kafka
```

| Layer | Convention | Example |
|---|---|---|
| **Endpoint** | `*Endpoint` | `WalletEndpoint` |
| **Use case** | `$ActionVerb$UseCase` + `execute` | `CreateWalletOnboardingUseCase` |
| **DTO** | `Create*RequestDto` / `Get*ResponseDto` | `CreateWalletOnboardRequestDto` |
| **PO** | `entity/po/**` | `Wallet`, `Account`, `LedgerMovement` |
| **Repository** | `*Repository` | `WalletRepository` |
| **Field names** | type camelCase | `ledgerMovementRepository` |
| **JSON** | camelCase | `ownerId`, `settlementCurrency` |

## Core tables (simplified)

### `wallet`

Customer-facing root: `owner_id` (unique — **1 CUST : 1 Wallet**), `settlement_currency` (default settlement), `account_id` (primary account), `coa_profile_code`, optional `vanity_code`.  

### `account`

COA + live balances: segments (`entity`, `type`, `sub_type`, `main_account`, `buffer`), `full_number`, `currency`, `ledger_balance`, `available_balance`.

### `ledger_movement` / `ledger_entry`

Business operation log (`movement_key` idempotency, mode AUTO/MANUAL, status) and settled legs (credit/debit).

## Main product APIs

| Area | Examples |
|---|---|
| **Wallets** | `POST /wallets`, `POST /wallets/batch`, `GET /wallets` (list), `GET /wallets/{ownerId}`, `GET /wallets?ownerId=` |
| **Movements** | `POST /movements/deposits`, `/withdrawals`, `/transfers/in-wallet`, settle/get/list |
| **Ledger accounts** | COA via `/coa-profiles`; books on `GET /wallets/{ownerId}` |
| **Money rails** | `/ledger/deposits`, `/ledger/withdrawals`, `/ledger/wallet-transfers/in-wallet` · rules, FX, configs |
| **Integration** | `POST /integrations/webhooks/transactions` (earn/burn rules) |

## Wallet create (curl)

### One customer → one wallet

`POST /wallets` opens **1 wallet** per `ownerId` (CRM CUST id).

- Always opens a **primary** account in `settlementCurrency` (e.g. HKD).
- Optional `accounts[]` adds extra **currency books** under the same wallet (typical: LP).
- Optional `coaProfileCode` selects numeric COA segments (blank → default profile).
- Optional `vanityCode` is display-only.

Each extra `accounts` entry:

```json
{ "currency": "LP", "name": "Loyalty points", "refCode": "LP" }
```

| Field | Notes |
|---|---|
| `currency` | Extra book currency (primary uses `settlementCurrency`) |
| `refCode` | Optional leaf code; blank + no currency → treated as primary |
| `name` | Optional display name |
| `primary` | `true` = main account (`wallet.accountId`); omit for extra lines |
| `allowNegative` | Default `false` |

Omitted / empty `accounts` → **primary only**. Duplicate extra currencies are skipped.

Numeric COA `fullNumber` (no English keys):

```text
fullNumber = entity(2) + type(2) + subType(2) + mainAccount + buffer(2) + currency(3)
example    = 10 + 20 + 00 + 10001 + 0000 + 00 + 344   →  10200010001000000344  (HKD primary)
```

One wallet = one `mainAccount`; extra books = extra leaves (`0000` primary).

```bash
# primary only (settlement HKD)
curl -sS -X POST 'http://localhost:8080/wallets' \
  -H 'Content-Type: application/json' \
  -d '{
    "ownerId": "01A47158227",
    "settlementCurrency": "HKD",
    "name": "Wilfill Kick"
  }'
```

```bash
# primary HKD + LP book (typical loyalty onboard)
curl -sS -X POST 'http://localhost:8080/wallets' \
  -H 'Content-Type: application/json' \
  -d '{
    "ownerId": "01A47158227",
    "settlementCurrency": "HKD",
    "name": "Wilfill Kick",
    "coaProfileCode": "DEFAULT",
    "accounts": [
      { "currency": "LP", "name": "Loyalty points", "refCode": "LP" }
    ]
  }'
```

```bash
# batch (soft-idempotent, max 1000)
curl -sS -X POST 'http://localhost:8080/wallets/batch' \
  -H 'Content-Type: application/json' \
  -d '{
    "wallets": [
      {
        "ownerId": "01A47158227",
        "settlementCurrency": "HKD",
        "name": "Wilfill Kick",
        "accounts": [
          { "currency": "LP", "name": "Loyalty points", "refCode": "LP" }
        ]
      },
      {
        "ownerId": "01A26182952",
        "settlementCurrency": "HKD",
        "name": "Wayne Yu",
        "accounts": [
          { "currency": "LP", "refCode": "LP" }
        ]
      }
    ]
  }'
```

```bash
# query by ownerId
curl -sS 'http://localhost:8080/wallets/01A47158227'
curl -sS 'http://localhost:8080/wallets?ownerId=01A47158227'
# optional: filter books
curl -sS 'http://localhost:8080/wallets/01A47158227?currencies=LP'
```

Required: `ownerId`, `settlementCurrency` (`HKD`, `USD`, `LP`, …).  
Response: `ownerId`, `settlementCurrency`, primary `account` / `balance`, full `accounts[]`.  
Envelope: `Result` with `response` / `data` / `requestId`. Duplicate `ownerId` → `WAL0409`.

**PROD bulk:** stream CRM CUST ids into `POST /wallets/batch` (≤1000/chunk, soft-idempotent).

## Currency

`com.altech.core.constant.enu.Currency` + `CurrencyType`:

| Type | Examples |
|---|---|
| **FIAT** | `USD`, `HKD`, `EUR`, `JPY`, … |
| **LOYALTY_POINT** | `LP` |
| **CRYPTO** | `BTC`, `ETH`, `SOL`, `USDT`, `USDC`, … |

Wallet default settlement uses JSON `settlementCurrency` (e.g. `"settlementCurrency": "USD"`).  
Account / movement amounts still use `"currency"`. Stored as `@Enumerated(STRING)`.

## Boundaries

| In scope | Out of scope |
|---|---|
| Wallets, accounts, balances, movements | Payment gateway / card rails |
| Idempotent movement keys | Full CRM customer master |
| Optional Kafka movement/integration events | Compliance UI, notifications |
| Fiat / LP / crypto units | Multi-party settlement orchestration |

Customer identity and names live in CRM; ledger stores **`ownerId`** (CRM CUST id) plus optional `vanityCode`.

## Docs

**唯一入口 → [docs/BOOKLET.md](docs/BOOKLET.md)**

| Doc | Purpose |
|-----|---------|
| [docs/BOOKLET.md](docs/BOOKLET.md) | **Start here** — map + happy path |
| [docs/BUSINESS_SHOOT_EXAMPLE.md](docs/BUSINESS_SHOOT_EXAMPLE.md) | 射單 Ingest→Digest→Book + COA |
| [docs/SYSTEM_BUSINESS_FLOW.md](docs/SYSTEM_BUSINESS_FLOW.md) | Product business flow |
| [PRODUCT.md](PRODUCT.md) | Product model summary |
| [INTEGRATION.md](INTEGRATION.md) | Integration summary |
| [docs/TECH_DEBT.md](docs/TECH_DEBT.md) | Deferred work |
| [docs/archive/](docs/archive/) | Old briefs / long scenarios (not daily) |
| [scripts/upstream-sim.sh](scripts/upstream-sim.sh) | One-cmd upstream sim |
| [scripts/e2e-smoke.sh](scripts/e2e-smoke.sh) | bootstrap → earn smoke |
| `application.yml` | Env-driven infra |
