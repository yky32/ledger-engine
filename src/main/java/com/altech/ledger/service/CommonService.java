package com.altech.ledger.service;

import com.altech.ledger.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CommonService {
    private final AccountRepository accountRepository;

    @Transactional(readOnly = true)
    public String getNextMainAccount() {
        long max = 10000L;
        for (String value : accountRepository.allMainAccountNumbers()) {
            try {
                max = Math.max(max, Long.parseLong(value.trim()));
            } catch (NumberFormatException ignored) {
                // skip non-numeric main account
            }
        }
        return String.valueOf(max + 1);
    }

    @Transactional(readOnly = true)
    public String getNextSubAccount(String mainAccount) {
        long max = 0L;
        for (String value : accountRepository.allSubAccountNumbers(mainAccount)) {
            try {
                max = Math.max(max, Long.parseLong(value.trim()));
            } catch (NumberFormatException ignored) {
                // skip
            }
        }
        return String.format("%04d", max + 1);
    }
}
