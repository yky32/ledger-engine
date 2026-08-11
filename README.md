# Ledger Engine

**Version `1.0.0`** — standalone wallet + ledger core.  
Optional client: [ledger-engine-sdk](https://github.com/yky32/ledger-engine-sdk) (manual JAR delivery; see [DELIVERY](https://github.com/yky32/ledger-engine-sdk/blob/main/docs/DELIVERY.md)).

Java 17 / Spring Boot service for **customer wallets**, **chart-of-accounts balances**, and **movements** (deposit, withdrawal, transfer, earn/burn). It is **ledger core**, not payment rails, identity/CRM, compliance UI, or settlement orchestration.

Product depth and integration flows: **[PRODUCT.md](PRODUCT.md)**, **[INTEGRATION.md](INTEGRATION.md)**.

## Model (strong play)

```text
Customer (CRM id only)     →  ownerId / associatedIdentifier on wallet
        │
     Wallet                →  1 customer : 1 wallet (`settlementCurrency` on row)
        │
     Account               →  1 primary COA balance row (settlement currency) at onboard
        │
     LedgerMovement        →  business ops (idempotent movementKey)
        └── LedgerEntry    →  applied credit/debit legs
```

- **Balances** live on `account` (`ledger_balance`, `available_balance`); movements update them under lock.
- **Wallet.settlementCurrency** = default settlement currency only (not a uniqueness key).
- **Unique wallet:** `(owner_id)` — 1 CUST : 1 Wallet.
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
| **JSON** | camelCase | `associatedIdentifier`, `settlementCurrency` |

## Core tables (simplified)

### `wallet`

Customer-facing root: `owner_id` (unique — **1 CUST : 1 Wallet**), `settlement_currency` (default settlement), `account_id` (primary account), status, external CRM ids.  

### `account`

COA + live balances: segments (`entity`, `type`, `sub_type`, `main_account`, `sub_account`, `buffer`), `full_number`, `currency`, `ledger_balance`, `available_balance`.

### `ledger_movement` / `ledger_entry`

Business operation log (`movement_key` idempotency, mode AUTO/MANUAL, status) and settled legs (credit/debit).

## Main product APIs

| Area | Examples |
|---|---|
| **Wallets** | `POST /wallets`, `POST /wallets/batch`, `GET /wallets/{ownerId}`, `GET /wallets?ownerId=` |
| **Movements** | `POST /movements/deposits`, `/withdrawals`, `/transfers/in-wallet`, settle/get/list |
| **Ledger accounts** | `POST /accounts`, get balance (product COA path) |
| **Parity** | `/ledger-wallets`, `/ledger-accounts`, `/ledger/deposits`, rules, FX, configs |
| **Integration** | `POST /integrations/webhooks/transactions` (earn/burn rules) |

## Wallet create (curl)

### One customer → one wallet + one primary account

`POST /wallets` opens **1 wallet** and **1 primary account** in the request `settlementCurrency`. `accounts` in the body is ignored for now.

Each `accounts` entry:

```json
{ "refCode": "LOAN", "name": "Loan line", "primary": false, "allowNegative": true }
```

| Field | Notes |
|---|---|
| `refCode` | Opaque suffix under the wallet base ref (SDK-defined). Blank → primary |
| `name` | Optional display name for the account |
| `primary` | Primary account (`wallet.accountId`); blank `refCode` also means primary |
| `allowNegative` | Default `false` |

Omitted / empty `accounts` → **primary only**. Primary is always ensured first; duplicate `refCode` ignored.

Account COA is **numeric only** (no English keys):

```text
fullNumber = entity(2) + type(2) + subType(2) + mainAccount + subAccount(4) + buffer(2) + currency(3)
example    = 10 + 20 + 00 + 10001 + 0000 + 00 + 344   →  10200010001000000344  (HKD primary)
```

- One wallet = one `mainAccount`; product lines = leaf `subAccount` (`0000` primary; numeric `refCode` → e.g. `89` → `0089`).
- Customer identity stays on wallet (`associatedIdentifier`), not in COA.

**Customer unique field:** `associatedIdentifier` only (CRM cust id). Stored as wallet `ownerId` + `associatedIdentifier`.  
Optional `associatedFrom` (default `CRM`).

```bash
# 1 CUST → 1 wallet + 1 primary account
curl -sS -X POST 'http://localhost:8080/wallets' \
  -H 'Content-Type: application/json' \
  -d '{
    "associatedIdentifier": "CUST-1001",
    "settlementCurrency": "USD",
    "name": "Primary wallet",
    "associatedFrom": "CRM"
  }'
```

```bash
# batch (soft-idempotent, max 1000)
curl -sS -X POST 'http://localhost:8080/wallets/batch' \
  -H 'Content-Type: application/json' \
  -d '{
    "wallets": [
      {
        "associatedIdentifier": "CUST-1001",
        "settlementCurrency": "HKD",
        "name": "Customer 1001"
      },
      {
        "associatedIdentifier": "CUST-1002",
        "settlementCurrency": "HKD",
        "name": "Customer 1002"
      }
    ]
  }'
```

```bash
# query by same customer id (path ownerId == associatedIdentifier)
curl -sS 'http://localhost:8080/wallets/CUST-1001'
curl -sS 'http://localhost:8080/wallets?ownerId=CUST-1001'
```

Required: `associatedIdentifier`, `settlementCurrency` (`USD`, `HKD`, `LP`, `BTC`, `USDT`, …).  
Response: `ownerId` / `associatedIdentifier` (same CRM id) + `settlementCurrency` + primary `account` / `balance`.  
Envelope: `Result` with `response` / `data` / `requestId`.

**PROD bulk convert:** stream CRM ids into `POST /wallets/batch` (≤1000/chunk, soft-idempotent).

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

Customer identity and names live in CRM; ledger only stores **`ownerId`** (and optional external ids on the wallet).

## Docs

| Doc | Purpose |
|-----|---------|
| [PRODUCT.md](PRODUCT.md) | Product model, operations, account roles |
| [INTEGRATION.md](INTEGRATION.md) | Onboarding + transactional event ingest |
| [docs/CLIENT_WALLET_ONBOARDING.md](docs/CLIENT_WALLET_ONBOARDING.md) | **Client adopt:** wallet create + query curls (HKD + LP) |
| [docs/CLIENT_EARN_WEBHOOK.md](docs/CLIENT_EARN_WEBHOOK.md) | **Client adopt:** earn webhook gates → LP credit |
| [docs/PHASE_A_ACCOUNTSET_COA.md](docs/PHASE_A_ACCOUNTSET_COA.md) | Phase A AccountSet + HKD/LP CoA roles |
| [scripts/e2e-smoke.sh](scripts/e2e-smoke.sh) | One-shot: onboard → earn → query LP → fail-table API |
| `application.yml` | Env-driven config (`DB_*`, `SERVER_PORT`, …) |
