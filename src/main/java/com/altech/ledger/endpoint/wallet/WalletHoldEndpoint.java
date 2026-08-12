package com.altech.ledger.endpoint.wallet;

import com.altech.core.response.R;
import com.altech.core.response.Result;
import com.altech.ledger.entity.dto.request.CreateHoldReleaseRequestDto;
import com.altech.ledger.entity.dto.response.GetLedgerMovementResponseDto;
import com.altech.ledger.usecase.wallet.HoldReleaseUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Hold / release available balance (ledger unchanged).
 */
@RestController
@RequestMapping("/wallets")
@RequiredArgsConstructor
public class WalletHoldEndpoint {
    private final HoldReleaseUseCase holdReleaseUseCase;

    @PostMapping("/holds")
    public Result<GetLedgerMovementResponseDto> hold(@Valid @RequestBody CreateHoldReleaseRequestDto body) {
        return R.success(holdReleaseUseCase.hold(body));
    }

    @PostMapping("/releases")
    public Result<GetLedgerMovementResponseDto> release(@Valid @RequestBody CreateHoldReleaseRequestDto body) {
        return R.success(holdReleaseUseCase.release(body));
    }
}
