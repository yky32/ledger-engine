package com.altech.ledger.service;

import com.altech.ledger.entity.enu.VirtualAccountStatus;
import com.altech.ledger.entity.enu.VirtualAccountType;
import com.altech.ledger.entity.po.ledger.VirtualAccount;
import com.altech.ledger.entity.po.ledger.VirtualSubAccount;
import com.altech.ledger.repository.VirtualAccountRepository;
import com.altech.ledger.repository.VirtualSubAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Port of the-wallet-ledger VirtualAccountService.
 */
@Service
public class VirtualAccountService {
    private final VirtualAccountRepository virtualAccounts;
    private final VirtualSubAccountRepository subAccounts;

    public VirtualAccountService(VirtualAccountRepository virtualAccounts,
                                 VirtualSubAccountRepository subAccounts) {
        this.virtualAccounts = virtualAccounts;
        this.subAccounts = subAccounts;
    }

    @Transactional
    public VirtualAccount createWithSubAccounts(String extIdentifier, String extType,
                                                VirtualAccountType type, String nickName,
                                                List<String> currencies) {
        VirtualAccount va = new VirtualAccount(extIdentifier, extType, type, nickName);
        va.setStatus(VirtualAccountStatus.APPROVED);
        va = virtualAccounts.save(va);
        List<String> ccys = currencies == null || currencies.isEmpty()
            ? List.of("USD", "HKD", "EUR", "GBP", "SGD") : currencies;
        for (String ccy : ccys) {
            subAccounts.save(new VirtualSubAccount(va, ccy.toUpperCase()));
        }
        return va;
    }

    @Transactional(readOnly = true)
    public List<VirtualSubAccount> subAccounts(Long virtualAccountId) {
        return subAccounts.findByVirtualAccountId(virtualAccountId);
    }
}
