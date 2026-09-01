package com.altech.ledger.endpoint.coa;

import com.altech.core.response.R;
import com.altech.core.response.Result;
import com.altech.ledger.entity.dto.request.AssignHouseWalletRequestDto;
import com.altech.ledger.entity.dto.response.GetHouseBooksResponseDto;
import com.altech.ledger.usecase.coa.HouseBooksUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * One company wallet + HOUSE_* books. Matches admin {@code /corporate-coa}.
 */
@RestController
@RequestMapping("/corporate-coa")
@RequiredArgsConstructor
public class CorporateCoaEndpoint {
    private final HouseBooksUseCase houseBooksUseCase;

    @GetMapping
    public Result<GetHouseBooksResponseDto> get() {
        return R.success(houseBooksUseCase.get());
    }

    /** createIfNotFound house COA, company wallet, and accounts. */
    @PostMapping
    public Result<GetHouseBooksResponseDto> ensure(
        @RequestBody(required = false) AssignHouseWalletRequestDto body
    ) {
        String ownerId = body == null ? null : body.ownerId();
        return R.success(houseBooksUseCase.ensure(ownerId));
    }
}
