package com.altech.ledger.usecase.coa;

import com.altech.core.constant.enu.Currency;
import com.altech.core.exception.BizException;
import com.altech.ledger.entity.dto.request.AccountOpenSpecDto;
import com.altech.ledger.entity.dto.request.CreateWalletOnboardRequestDto;
import com.altech.ledger.entity.dto.response.GetCoaProfileResponseDto;
import com.altech.ledger.entity.dto.response.GetWalletOnboardResponseDto;
import com.altech.ledger.entity.dto.response.UafDemoSeedResponseDto;
import com.altech.ledger.exception.response.WalletErrorResponse;
import com.altech.ledger.repository.WalletRepository;
import com.altech.ledger.usecase.wallet.CreateWalletOnboardingUseCase;
import com.altech.ledger.usecase.wallet.QueryWalletUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * One-shot UAF demo: profiles UAF_CC/UAF_LOAN + two sample wallets.
 */
@Component
@RequiredArgsConstructor
public class UafDemoSeedUseCase {
    public static final String CODE_CC = "UAF_CC";
    public static final String CODE_LOAN = "UAF_LOAN";
    public static final String DEMO_CARD_OWNER = "UAF-CARD-DEMO";
    public static final String DEMO_LOAN_OWNER = "UAF-LOAN-DEMO";

    private final CoaProfileUseCase coaProfileUseCase;
    private final CreateWalletOnboardingUseCase createWalletOnboardingUseCase;
    private final QueryWalletUseCase queryWalletUseCase;
    private final WalletRepository walletRepository;

    @Transactional
    public UafDemoSeedResponseDto execute() {
        coaProfileUseCase.requireDefault();
        List<GetCoaProfileResponseDto> profiles = new ArrayList<>();
        profiles.add(coaProfileUseCase.ensureProfile(CODE_CC, "UAF Credit Card stream", "01", false));
        profiles.add(coaProfileUseCase.ensureProfile(CODE_LOAN, "UAF Loan stream", "02", false));

        List<GetWalletOnboardResponseDto> wallets = new ArrayList<>();
        wallets.add(_ensureWallet(DEMO_CARD_OWNER, "UAF Card Demo", CODE_CC));
        wallets.add(_ensureWallet(DEMO_LOAN_OWNER, "UAF Loan Demo", CODE_LOAN));

        return UafDemoSeedResponseDto.builder()
            .profiles(profiles)
            .wallets(wallets)
            .note("entity 01=CC · 02=Loan · owners " + DEMO_CARD_OWNER + " / " + DEMO_LOAN_OWNER)
            .build();
    }

    private GetWalletOnboardResponseDto _ensureWallet(String ownerId, String name, String coaCode) {
        if (walletRepository.existsByOwnerId(ownerId)) {
            return queryWalletUseCase.byOwnerId(ownerId);
        }
        try {
            return createWalletOnboardingUseCase.execute(new CreateWalletOnboardRequestDto(
                ownerId,
                Currency.HKD,
                name,
                null,
                coaCode,
                List.of(new AccountOpenSpecDto("LP", "LP book", false, false, Currency.LP))
            ));
        } catch (BizException ex) {
            String code = ex.getResponse() != null ? ex.getResponse().getCode() : null;
            if (WalletErrorResponse.WAL0409.getCode().equals(code)) {
                return queryWalletUseCase.byOwnerId(ownerId);
            }
            throw ex;
        }
    }
}
