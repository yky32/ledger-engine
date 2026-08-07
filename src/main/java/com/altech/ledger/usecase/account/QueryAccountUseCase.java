package com.altech.ledger.usecase.account;

import com.altech.ledger.repository.AccountRepository;
import com.altech.ledger.service.DtoMapper;
import com.altech.ledger.usecase.CommonUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.altech.ledger.entity.dto.response.GetLedgerAccountResponseDto;

@Component
@RequiredArgsConstructor
public class QueryAccountUseCase {
    private final AccountRepository accountRepository;
    private final CommonUseCase commonUseCase;

    @Transactional(readOnly = true)
    public GetLedgerAccountResponseDto one(Long id) {
        return DtoMapper.toAccount(commonUseCase.requireAccount(id));
    }

    @Transactional(readOnly = true)
    public Page<GetLedgerAccountResponseDto> list(Pageable pageable) {
        return accountRepository.findAll(pageable).map(DtoMapper::toAccount);
    }
}
