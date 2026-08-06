package com.altech.ledger.usecase.account.virtual;

import com.altech.ledger.entity.dto.parity.ParityDtos.VirtualAccountResponse;
import com.altech.ledger.entity.po.ledger.VirtualAccount;
import com.altech.ledger.repository.VirtualAccountRepository;
import com.altech.ledger.repository.VirtualSubAccountRepository;
import com.altech.ledger.service.DtoMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Port of the-wallet-ledger VirtualAccountUseCase (account listing only; applications separate).
 */
@Service
public class VirtualAccountUseCase {
    private final VirtualAccountRepository virtualAccounts;
    private final VirtualSubAccountRepository subAccounts;

    public VirtualAccountUseCase(VirtualAccountRepository virtualAccounts,
                                 VirtualSubAccountRepository subAccounts) {
        this.virtualAccounts = virtualAccounts;
        this.subAccounts = subAccounts;
    }

    @Transactional(readOnly = true)
    public Page<VirtualAccountResponse> list(Pageable pageable) {
        return virtualAccounts.findAll(pageable).map(this::toVa);
    }

    @Transactional(readOnly = true)
    public List<VirtualAccountResponse> me(String extIdentifier) {
        return virtualAccounts.findByExtIdentifier(extIdentifier)
            .map(va -> List.of(toVa(va)))
            .orElse(List.of());
    }

    private VirtualAccountResponse toVa(VirtualAccount va) {
        return DtoMapper.toVirtualAccount(va, subAccounts.findByVirtualAccountId(va.getId()));
    }
}
