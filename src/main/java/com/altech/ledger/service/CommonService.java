package com.altech.ledger.service;

import com.altech.ledger.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommonService {
    private final AccountRepository accounts;

    public CommonService(AccountRepository accounts) {
        this.accounts = accounts;
    }

    @Transactional(readOnly = true)
    public String getNextMainAccount() {
        long max = 10000L;
        for (String value : accounts.allMainAccountNumbers()) {
            try {
                max = Math.max(max, Long.parseLong(value.trim()));
            } catch (NumberFormatException ignored) {
                // skip non-numeric main accounts
            }
        }
        return String.valueOf(max + 1);
    }

    @Transactional(readOnly = true)
    public String getNextSubAccount(String mainAccount) {
        long max = 0L;
        for (String value : accounts.allSubAccountNumbers(mainAccount)) {
            try {
                max = Math.max(max, Long.parseLong(value.trim()));
            } catch (NumberFormatException ignored) {
                // skip
            }
        }
        return String.format("%04d", max + 1);
    }
}
