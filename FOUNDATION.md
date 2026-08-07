# the-wallet-ledger → ledger-engine parity

**Core rule:** if the-wallet-ledger has it → we implement it (standalone).

## Class map (old → engine)

| the-wallet-ledger | ledger-engine |
|---|---|
| `BaseLedgerMovementShooter` | `usecase.BaseLedgerMovementShooter` |
| `LedgerMovementShooter` | `usecase.ledger.LedgerMovementShooter` |
| `LedgerDepositUseCase` | `usecase.ledger.LedgerDepositUseCase` |
| `LedgerMovementUseCase` | `usecase.ledger.LedgerMovementUseCase` |
| `LedgerMovementQueryUseCase` | `usecase.ledger.LedgerMovementQueryUseCase` |
| `LedgerMovementOperationUseCase` | `usecase.ledger.LedgerMovementOperationUseCase` |
| `LedgerMovementExecutionUseCase` | `usecase.ledger.LedgerMovementExecutionUseCase` |
| `AccountSetupUseCase` | `usecase.setup.AccountSetupUseCase` |
| `AccountOperationUseCase` | `usecase.account.AccountOperationUseCase` |
| `WalletSetupUseCase` | `usecase.setup.WalletSetupUseCase` |
| `WalletAccountBalanceUseCase` | `usecase.account.WalletAccountBalanceUseCase` |
| `MyWalletUseCase` | `usecase.MyWalletUseCase` |
| `FxRateSetupUseCase` | `usecase.FxRateSetupUseCase` |
| `FxRateQueryUseCase` | `usecase.FxRateQueryUseCase` |
| `RuleSetupUseCase` | `usecase.setup.RuleSetupUseCase` |
| `RuleExecutionUseCase` | `usecase.account.RuleExecutionUseCase` |
| `RecipientSetupUseCase` | `usecase.setup.RecipientSetupUseCase` |
| `LinkedBankAccountUseCase` | `usecase.setup.LinkedBankAccountUseCase` |
| `WalletPaymentMethodUseCase` | `usecase.WalletPaymentMethodUseCase` |
| `VirtualAccountUseCase` | `usecase.account.virtual.VirtualAccountUseCase` |
| `VirtualAccountApplicationUseCase` | `usecase.account.virtual.VirtualAccountApplicationUseCase` |
| `SystemConfigurationUseCase` | `usecase.SystemConfigurationUseCase` |
| `WalletService` / `LedgerMovementService` / `VirtualAccountService` / `CommonService` | `service.*` |
| `LedgerHandler` | `listener.intf.LedgerHandler` |
| `LedgerMovementInitiatedListener` | `listener.log.LedgerMovementInitiatedListener` |
| `LedgerMovementDoneListener` | `listener.log.LedgerMovementDoneListener` |
| `LedgerMovementEventListener` | `listener.LedgerMovementEventListener` |
| `WalletAccountSetupListener` | `listener.usecase.WalletAccountSetupListener` |
| `RecipientSetupListener` | `listener.usecase.RecipientSetupListener` |
| `FileMetadata` / `ComplianceContext` / deposit detail | `entity.json_context.*` |
| `DtoWrapper` | `service.DtoWrapper` (+ `DtoMapper`) |
| `BalanceExecutionResultCommand` | `entity.dto.BalanceExecutionResultCommand` |

## REST + pipeline

- All legacy path domains implemented.
- Multipart deposit/transfer/compliance/admin SWIFT.
- Movement query supports `startDt`, `endDt`, `statuses`.
- Kafka optional (`ledger.movement.kafka.*`); sync default.
- Earn/Burn: journal + `LedgerMovement` log.
- Execution: `rulesExecution` → `BalanceExecutionResultCommand` → apply balances.
- Linked-bank deposit target → skip wallet credit (old behaviour).
- Deposit accepts legacy `targetId`; withdrawal accepts `originatorId`.

## json_context

`Bank`, `CreditCard`, `PaymentMethodMetadata`, `RuleMetadata`, `RuleExecutionMetadata`, transfer originator/target, payer/recipient, remarks.

## Standalone (not vendor lock-in)

OAuth / GrandPay / IDV / S3 / Discord → params, activate stub, file metadata, logs.

## Tests

`mvn test` — **21 green · BUILD SUCCESS** (`DeepParityIntegrationTest` + prior suites).
