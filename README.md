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

```bash
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
# batch (soft-idempotent, max 1000)
curl -sS -X POST 'http://localhost:8080/wallets/batch' \
  -H 'Content-Type: application/json' \
  -d '{
    "wallets": [
      { "userId": "CUST-1001", "currency": "USD" },
      { "userId": "CUST-1002", "currency": "LP" }
    ]
  }'
```

```bash
curl -sS 'http://localhost:8080/wallets/CUST-1001/USD'
curl -sS 'http://localhost:8080/wallets?ownerId=CUST-1001'
```

Required: `userId`, `currency` (`USD`, `HKD`, `LP`, `BTC`, `USDT`, …).  
Response envelope: `Result` with `response` / `data` / `requestId`.

**Note:** product onboard creates **1 wallet + 1 primary account** today. Enterprise target is **wallet → account-set** (multiple COA lines under one wallet); expand via hierarchical account APIs or a future product template.

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
