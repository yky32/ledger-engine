package com.altech.ledger.usecase.ledger;

import com.altech.core.exception.BizException;
import com.altech.ledger.entity.dto.posting.PostingCommand;
import com.altech.ledger.entity.dto.request.CreateLedgerDepositRequestDto;
import com.altech.ledger.entity.dto.request.CreateLedgerInWalletTransferRequestDto;
import com.altech.ledger.entity.dto.request.CreateLedgerWithdrawalRequestDto;
import com.altech.ledger.entity.dto.response.GetLedgerMovementResponseDto;
import com.altech.ledger.exception.response.MovementErrorResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Central apply-posting use case — reusable entry for all balance-affecting intents.
 * Book mutations: {@code OperateAccountBalanceUseCase.deposit} / {@code withdrawal}.
 * Transfer / earn / burn compose those two (one debit leg, one credit leg).
 */
@Component
@RequiredArgsConstructor
public class ApplyPostingUseCase {
    private final LedgerMovementShooter ledgerMovementShooter;

    @Transactional
    public GetLedgerMovementResponseDto execute(PostingCommand cmd) {
        if (cmd == null) {
            throw new BizException(MovementErrorResponse.MOV0400, "PostingCommand required");
        }
        if (cmd.walletId() == null) {
            throw new BizException(MovementErrorResponse.MOV0400, "walletId required");
        }
        if (cmd.amount() == null) {
            throw new BizException(MovementErrorResponse.MOV0400, "amount required");
        }
        if (cmd.currency() == null) {
            throw new BizException(MovementErrorResponse.MOV0400, "currency required");
        }
        if (cmd.intent() == null) {
            throw new BizException(MovementErrorResponse.MOV0400, "intent required");
        }

        return switch (cmd.intent()) {
            case DEPOSIT -> ledgerMovementShooter.doDeposit(new CreateLedgerDepositRequestDto(
                String.valueOf(cmd.walletId()),
                cmd.currency(),
                cmd.amount(),
                cmd.mode(),
                null,
                cmd.movementKey(),
                cmd.description(),
                null
            ));
            case WITHDRAWAL -> ledgerMovementShooter.doWithdrawal(new CreateLedgerWithdrawalRequestDto(
                String.valueOf(cmd.walletId()),
                cmd.currency(),
                cmd.amount(),
                cmd.mode(),
                cmd.externalPartyId(),
                cmd.movementKey(),
                cmd.description()
            ));
            case IN_WALLET_TRANSFER -> {
                if (cmd.counterpartyWalletId() == null) {
                    throw new BizException(MovementErrorResponse.MOV0400,
                        "counterpartyWalletId required for IN_WALLET_TRANSFER");
                }
                yield ledgerMovementShooter.doInWalletTransfer(new CreateLedgerInWalletTransferRequestDto(
                    String.valueOf(cmd.walletId()),
                    String.valueOf(cmd.counterpartyWalletId()),
                    cmd.currency(),
                    cmd.amount(),
                    cmd.mode(),
                    cmd.movementKey(),
                    cmd.description()
                ));
            }
            case EARN, BURN -> ledgerMovementShooter.doEarnBurn(
                cmd.walletId(),
                cmd.intent().toOrderType(),
                cmd.amount(),
                cmd.currency(),
                cmd.movementKey(),
                cmd.description(),
                cmd.accountId(),
                cmd.eventType(),
                cmd.mainAccount()
            );
            case HOLD, RELEASE -> ledgerMovementShooter.doHoldRelease(
                cmd.walletId(),
                cmd.intent().toOrderType(),
                cmd.amount(),
                cmd.currency(),
                cmd.movementKey(),
                cmd.description(),
                cmd.accountId()
            );
        };
    }
}
