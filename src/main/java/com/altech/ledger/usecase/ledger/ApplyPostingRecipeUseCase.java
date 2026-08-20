package com.altech.ledger.usecase.ledger;

import com.altech.core.constant.enu.Currency;
import com.altech.core.exception.BizException;
import com.altech.ledger.entity.dto.posting.PostingCommand;
import com.altech.ledger.entity.dto.posting.PostingRecipe;
import com.altech.ledger.entity.dto.response.GetLedgerMovementResponseDto;
import com.altech.ledger.entity.enu.PostingAtom;
import com.altech.ledger.exception.response.MovementErrorResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs a {@link PostingRecipe} as ordered atoms via {@link ApplyPostingUseCase}.
 * <p>
 * UA Finance COA (Expense DR / Custodian CR) maps operationally to PROGRAM↔member
 * double-entry for now; GL segment labels stay outside the engine.
 */
@Component
@RequiredArgsConstructor
public class ApplyPostingRecipeUseCase {
    private final ApplyPostingUseCase applyPostingUseCase;

    public record RecipeRunResult(
        String recipeCode,
        List<GetLedgerMovementResponseDto> steps,
        GetLedgerMovementResponseDto last
    ) {}

    @Transactional
    public RecipeRunResult execute(
        Long walletId,
        PostingRecipe recipe,
        BigDecimal amount,
        String movementKeyBase,
        String description
    ) {
        if (walletId == null || recipe == null) {
            throw new BizException(MovementErrorResponse.MOV0400, "walletId and recipe required");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new BizException(MovementErrorResponse.MOV0400, "amount must be positive");
        }
        String base = movementKeyBase == null || movementKeyBase.isBlank()
            ? "recipe-" + recipe.code()
            : movementKeyBase.trim();
        String desc = description == null ? recipe.code() : description;

        List<GetLedgerMovementResponseDto> steps = new ArrayList<>();
        Currency bookCcy = recipe.rewardCcy() != null ? recipe.rewardCcy() : Currency.LP;
        int i = 0;
        for (PostingAtom atom : recipe.atoms()) {
            i++;
            String key = base + "-" + atom.name().toLowerCase() + "-" + i;
            String stepDesc = desc + " [" + recipe.code() + "/" + atom + "]";
            GetLedgerMovementResponseDto m = switch (atom) {
                case CREDIT_REWARD -> applyPostingUseCase.execute(PostingCommand.earn(
                    walletId, amount, bookCcy, key, stepDesc));
                case REDEEM, CASHBACK -> applyPostingUseCase.execute(PostingCommand.burn(
                    walletId, amount, bookCcy, key, stepDesc));
                case CONVERT_HKD_TO_LP -> {
                    applyPostingUseCase.execute(PostingCommand.burn(
                        walletId, amount, Currency.HKD, key + "-out", stepDesc + " out-HKD"));
                    GetLedgerMovementResponseDto in = applyPostingUseCase.execute(PostingCommand.earn(
                        walletId, amount, Currency.LP, key + "-in", stepDesc + " in-LP"));
                    bookCcy = Currency.LP;
                    yield in;
                }
            };
            steps.add(m);
        }
        return new RecipeRunResult(recipe.code(), steps, steps.get(steps.size() - 1));
    }
}
