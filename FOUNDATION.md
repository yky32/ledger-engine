# ledger-engine foundation

**Core rule:** if a domain capability is required → implement it (standalone).

## Use-case pattern

| Pattern | Rule |
|---|---|
| `$ActionVerb$UseCase` | One primary action per class; mutating entry is `execute(...)` |
| `Query*UseCase` | Read surface; methods like `one` / `list` / domain-specific queries |
| Private helpers | Prefix with `_` (e.g. `_createWallet`, `_requireActive`) |
| `CommonUseCase` | Cross-cutting entity loaders / validators shared by Verb use cases |

## Class map

| | ledger-engine |
|---|---|
| Shared helpers | `usecase.CommonUseCase` |
| Base movement dispatch | `usecase.BaseLedgerMovementShooter` |
| Movement shooter | `usecase.ledger.LedgerMovementShooter` |
| Deposit | `usecase.ledger.LedgerDepositUseCase` |
| Log movement legs | `usecase.ledger.LedgerMovementUseCase` |
| Query movements (parity) | `usecase.ledger.LedgerMovementQueryUseCase` |
| Settle / status ops | `usecase.ledger.LedgerMovementOperationUseCase` |
| Execute balances | `usecase.ledger.LedgerMovementExecutionUseCase` |
| Create account (parity) | `usecase.account.CreateAccountUseCase` |
| Query account (parity) | `usecase.account.QueryAccountUseCase` |
| Create wallet (parity) | `usecase.wallet.CreateWalletUseCase` |
| Activate / update wallet | `usecase.wallet.ActivateWalletUseCase` / `UpdateWalletUseCase` |
| Query wallet balances | `usecase.account.QueryWalletBalanceUseCase` |
| My wallets | `usecase.wallet.QueryMyWalletUseCase` |
| Onboard wallet | `usecase.wallet.CreateWalletOnboardingUseCase` |
| Query onboard wallet | `usecase.wallet.QueryWalletUseCase` |
| FX create / update / query | `usecase.fx.*` |
| Rule create / query | `usecase.rule.*` |
| Rule execution create / query | `usecase.rule.CreateRuleExecutionUseCase` / `QueryRuleExecutionUseCase` |
| System config | `usecase.config.QuerySystemConfigurationUseCase` / `UpsertSystemConfigurationUseCase` |
| Product movements | `usecase.movement.CreateDepositUseCase` / `CreateWithdrawalUseCase` / `CreateInWalletTransferUseCase` / `SettleMovementUseCase` / `QueryMovementUseCase` |
| Product ledger accounts | `usecase.ledger.CreateLedgerAccountUseCase` / `QueryLedgerAccountUseCase` |
| Ingest loyalty events | `usecase.integration.IngestTransactionUseCase` |
| Services | `service.*` |
| Handlers / listeners | `listener.*` |
| DTO mappers | `service.DtoWrapper` (+ `DtoMapper`) |

## REST + pipeline

- Product + parity path domains implemented.
- Multipart deposit supported.
- Movement query supports `startDt`, `endDt`, `statuses`.
- Kafka optional (`ledger.movement.kafka.*`); sync default.
- Execution: `rulesExecution` → `BalanceExecutionResultCommand` → apply balances.

## Tests

`mvn test` — integration suites under `src/test/java`.
}
