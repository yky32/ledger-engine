# Ledger Engine

**Version `1.0.0`** — standalone wallet + ledger core.  
Optional client: [ledger-engine-sdk](https://github.com/yky32/ledger-engine-sdk) (manual JAR delivery; see [DELIVERY](https://github.com/yky32/ledger-engine-sdk/blob/main/docs/DELIVERY.md)).

Java 17 / Spring Boot service for **customer wallets**, **chart-of-accounts balances**, and **movements** (deposit, withdrawal, transfer, earn/burn). It is **ledger core**, not payment rails, identity/CRM, compliance UI, or settlement orchestration.

Product depth and integration flows: **[PRODUCT.md](PRODUCT.md)**, **[INTEGRATION.md](INTEGRATION.md)**.

## Model (strong play)

```text
Customer (CRM id only)     →  ownerId on wallet
        │
     Wallet                →  1 customer ledger root (per currency today)
        │
     Account set           →  1..N COA balance rows (primary + product lines)
        │
     LedgerMovement        →  business ops (idempotent movementKey)
        └── LedgerEntry    →  applied credit/debit legs
```

- **Balances** live on `account` (`ledger_balance`, `available_balance`); movements update them under lock.
- **Currency** is an enum (`fiat` / `LOYALTY_POINT` / `crypto`): `USD`, `HKD`, `LP`, `BTC`, `USDT`, …
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
| **JSON** | camelCase | `userId`, `currency` |

## Core tables (simplified)

### `wallet`

Customer-facing root: `owner_id`, `currency`, `account_id` (primary account), status, external CRM ids.  
Unique: `(owner_id, currency)`.

### `account`

COA + live balances: segments (`entity`, `type`, `sub_type`, `main_account`, `sub_account`, `buffer`), `full_number`, `currency`, `ledger_balance`, `available_balance`.

### `ledger_movement` / `ledger_entry`

Business operation log (`movement_key` idempotency, mode AUTO/MANUAL, status) and settled legs (credit/debit).

## Main product APIs

| Area | Examples |
|---|---|
| **Wallets** | `POST /wallets`, `POST /wallets/batch`, `GET /wallets?ownerId=`, `GET /wallets/{ownerId}/{currency}` |
| **Movements** | `POST /movements/deposits`, `/withdrawals`, `/transfers/in-wallet`, settle/get/list |
| **Ledger accounts** | `POST /accounts`, get balance (product COA path) |
| **Parity** | `/ledger-wallets`, `/ledger-accounts`, `/ledger/deposits`, rules, FX, configs |
| **Integration** | `POST /integrations/webhooks/transactions` (earn/burn rules) |

## Wallet create (curl)

### Flexible account-set

`POST /wallets` opens **1 wallet + N accounts**. Callers indicate product lines via optional `accountSet`.

| Role | Ref suffix | Typical use |
|---|---|---|
| `MAIN` | *(base ref)* | Primary balance (always opened; omit is fine) |
| `LOAN` | `:LOAN` | Loan facility |
| `CC_YELLOW` | `:88` | Credit card yellow |
| `CC_PURPLE` | `:89` | Credit card purple |
| `REWARD` | `:REWARD` | Separate reward/points line |

Each `accountSet` entry:

```json
{ "role": "LOAN", "productCode": null, "allowNegative": false }
```

- **`role`** (required) — which account kind to open  
- **`productCode`** (optional) — override default suffix (e.g. custom card id)  
- **`allowNegative`** (optional, default `false`) — overdraft / credit-style balances  

Omitted / empty `accountSet` → **MAIN only** (backward compatible).  
MAIN is always ensured first; duplicate roles are ignored (first wins).

Account refs: `wallet:{userId}:{currency}` for MAIN; others `wallet:{userId}:{currency}:{refCode}`.

```bash
# MAIN only (default)
curl -sS -X POST 'http://localhost:8080/wallets' \
  -H 'Content-Type: application/json' \
  -d '{
    "userId": "CUST-1001",
    "currency": "USD",
    "name": "Primary wallet",
    "externalId": "crm-1001",
    "externalType": "CRM"
  }'
```

```bash
# Loan + purple card (MAIN auto-added)
curl -sS -X POST 'http://localhost:8080/wallets' \
  -H 'Content-Type: application/json' \
  -d '{
    "userId": "01A47158227",
    "currency": "HKD",
    "name": "Wilfill Kick",
    "externalId": "01A47158227",
    "externalType": "CRM",
    "accountSet": [
      { "role": "MAIN" },
      { "role": "LOAN", "allowNegative": true },
      { "role": "CC_PURPLE", "allowNegative": true }
    ]
  }'
```

```bash
# batch (soft-idempotent, max 1000) — per-row accountSet for PROD bulk convert
curl -sS -X POST 'http://localhost:8080/wallets/batch' \
  -H 'Content-Type: application/json' \
  -d '{
    "wallets": [
      {
        "userId": "01A47158227",
        "currency": "HKD",
        "name": "Wilfill Kick",
        "accountSet": [
          { "role": "MAIN" },
          { "role": "LOAN", "allowNegative": true },
          { "role": "CC_PURPLE", "allowNegative": true }
        ]
      },
      {
        "userId": "01A26182952",
        "currency": "HKD",
        "name": "Wayne Yu",
        "accountSet": [
          { "role": "MAIN" },
          { "role": "CC_YELLOW", "allowNegative": true }
        ]
      },
      {
        "userId": "01A76786421",
        "currency": "HKD",
        "name": "Eric Chow",
        "accountSet": [
          { "role": "MAIN" },
          { "role": "CC_YELLOW", "allowNegative": true },
          { "role": "CC_PURPLE", "allowNegative": true }
        ]
      }
    ]
  }'
```

```bash
curl -sS 'http://localhost:8080/wallets/01A47158227/HKD'
curl -sS 'http://localhost:8080/wallets?ownerId=01A47158227'
```

Required: `userId`, `currency` (`USD`, `HKD`, `LP`, `BTC`, `USDT`, …).  
Response: wallet + primary `account` / `balance` + full `accounts[]` (each with `role`).  
Envelope: `Result` with `response` / `data` / `requestId`.

**PROD bulk convert:** stream CRM product mix into `POST /wallets/batch` (≤1000/chunk, soft-idempotent). No 700K SQL cutover / downtime.

## Currency

`com.altech.core.constant.enu.Currency` + `CurrencyType`:

| Type | Examples |
|---|---|
| **FIAT** | `USD`, `HKD`, `EUR`, `JPY`, … |
| **LOYALTY_POINT** | `LP` |
| **CRYPTO** | `BTC`, `ETH`, `SOL`, `USDT`, `USDC`, … |

JSON uses the code string, e.g. `"currency": "USD"`. Stored as `@Enumerated(STRING)`.

## Boundaries

| In scope | Out of scope |
|---|---|
| Wallets, accounts, balances, movements | Payment gateway / card rails |
| Idempotent movement keys | Full CRM customer master |
| Optional Kafka movement/integration events | Compliance UI, notifications |
| Fiat / LP / crypto units | Multi-party settlement orchestration |

Customer identity and names live in CRM; ledger only stores **`ownerId`** (and optional external ids on the wallet).

## Docs

| File | Purpose |
|---|---|
| [PRODUCT.md](PRODUCT.md) | Product model, operations, account roles |
| [INTEGRATION.md](INTEGRATION.md) | Onboarding + transactional event ingest |
| `application.yml` | Env-driven config (`DB_*`, `SERVER_PORT`, …) |
