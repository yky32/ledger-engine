package com.altech.ledger.endpoint.usecase.deposit;

import com.altech.core.constant.enu.Currency;
import com.altech.core.response.R;
import com.altech.core.response.Result;
import com.altech.ledger.entity.enu.LedgerMovementMode;
import com.altech.ledger.usecase.ledger.LedgerDepositUseCase;
import com.altech.ledger.util.MultipartFileMetadata;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import com.altech.ledger.entity.dto.request.CreateLedgerDepositRequestDto;
import com.altech.ledger.entity.dto.response.GetLedgerMovementResponseDto;

@RestController
@RequiredArgsConstructor
public class DepositEndpoint {
    private final LedgerDepositUseCase ledgerDepositUseCase;

    @PostMapping("/ledger/deposits")
    public Result<GetLedgerMovementResponseDto> deposit(@Valid @RequestBody CreateLedgerDepositRequestDto dto) {
        return R.success(ledgerDepositUseCase.execute(dto));
    }

    @PostMapping(value = "/ledger/deposits", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<GetLedgerMovementResponseDto> depositMultipart(
        @RequestParam String targetWalletId,
        @RequestParam String currency,
        @RequestParam BigDecimal amount,
        @RequestParam(required = false) String movementKey,
        @RequestParam(required = false) String description,
        @RequestParam(required = false) List<MultipartFile> files
    ) {
        String fileMeta = MultipartFileMetadata.summarize(files);
        return R.success(ledgerDepositUseCase.execute(new CreateLedgerDepositRequestDto(
            targetWalletId, Currency.get(currency), amount, LedgerMovementMode.MANUAL, null, movementKey,
            description, fileMeta == null ? null : Map.of("files", fileMeta))));
    }

    @PostMapping("/ledger/deposits/card-session")
    public Result<Map<String, String>> cardSession(
        @RequestParam Long walletId,
        @RequestParam String currency,
        @RequestParam BigDecimal amount
    ) {
        return R.success(ledgerDepositUseCase.executeCardSession(walletId, currency, amount, Map.of()));
    }
}
