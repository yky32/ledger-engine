# Central posting (reusable balance engine)

> **Docs entry:** [START_HERE.md](./START_HERE.md)

## Why

Deposit / withdrawal / transfer / earn / burn / hold all **change balances**.
They must share one pipeline — but **not** the same accounting intent.

```text
Product / Ingest / Hold
        │
        ▼
  PostingCommand + PostingIntent
        │
        ▼
  ApplyPostingUseCase.execute(...)
        │
        ▼
  LedgerMovementShooter (create movement + execute)
        │
        ▼
  Execution rules by OrderType
        │
        ├─ DEPOSIT / WITHDRAWAL  → single-sided member
        ├─ EARN / BURN           → PROGRAM pool DE
        ├─ TRANSFER              → two wallets
        └─ HOLD / RELEASE        → available only
```

## Public API (code)

| Type | Role |
|------|------|
| `PostingIntent` | DEPOSIT · WITHDRAWAL · IN_WALLET_TRANSFER · EARN · BURN · HOLD · RELEASE |
| `PostingCommand` | amount · currency · walletId · movementKey · factories |
| `ApplyPostingUseCase#execute` | **Only** balance-write entry new code should call |

### Factories

```java
PostingCommand.deposit(walletId, amount, ccy, key, desc, mode);
PostingCommand.withdrawal(...);
PostingCommand.inWalletTransfer(from, to, ...);
PostingCommand.earn(...);   // PROGRAM DE
PostingCommand.burn(...);   // PROGRAM DE
PostingCommand.hold(...);
PostingCommand.release(...);

applyPostingUseCase.execute(cmd);
// or
applyPostingUseCase.earn(walletId, points, LP, key, desc);
```

## Callers (wired)

| Caller | Intent |
|--------|--------|
| `CreateDepositUseCase` | DEPOSIT |
| `CreateWithdrawalUseCase` | WITHDRAWAL |
| `CreateInWalletTransferUseCase` | IN_WALLET_TRANSFER |
| `IngestTransactionUseCase` | EARN / BURN |
| `HoldReleaseUseCase` | HOLD / RELEASE |
| `LedgerDepositUseCase` / pipeline | DEPOSIT / WITHDRAW / xfer |

## Do not

| ❌ | Why |
|----|-----|
| Earn via deposit API | No PROGRAM legs — false mint |
| Burn via withdrawal API | No PROGRAM reclaim |
| New balance code bypassing `ApplyPostingUseCase` | Drift |

## Related

[DOUBLE_ENTRY_EARN.md](./DOUBLE_ENTRY_EARN.md) · [HOLD_RELEASE.md](./HOLD_RELEASE.md) · [API_SURFACE.md](./API_SURFACE.md)
