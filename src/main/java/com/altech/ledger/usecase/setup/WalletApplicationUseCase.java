package com.altech.ledger.usecase.setup;

import com.altech.ledger.entity.enu.WalletApplicationStatus;
import com.altech.ledger.entity.po.ledger.WalletApplication;
import com.altech.ledger.exception.LedgerException;
import com.altech.ledger.repository.WalletApplicationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

/** Port of wallet application flow (minimal API over WalletApplication PO). */
@Service
public class WalletApplicationUseCase {
    private final WalletApplicationRepository applications;
    private final WalletSetupUseCase walletSetupUseCase;

    public WalletApplicationUseCase(WalletApplicationRepository applications,
                                    WalletSetupUseCase walletSetupUseCase) {
        this.applications = applications;
        this.walletSetupUseCase = walletSetupUseCase;
    }

    @Transactional
    public WalletApplication create(String extIdentifier, String extType, String requestBody, String alias) {
        String hash = sha256(extIdentifier + "|" + (extType == null ? "" : extType) + "|" + UUID.randomUUID());
        if (applications.findByReferenceHash(hash).isPresent()) {
            throw LedgerException.conflict("APPLICATION_EXISTS", "reference hash exists");
        }
        WalletApplication app = new WalletApplication(extIdentifier, extType, hash,
            alias == null ? extIdentifier : alias);
        app.setRequestBody(requestBody);
        return applications.save(app);
    }

    @Transactional(readOnly = true)
    public WalletApplication get(Long id) {
        return applications.findById(id)
            .orElseThrow(() -> LedgerException.notFound("WalletApplication not found: " + id));
    }

    @Transactional(readOnly = true)
    public Page<WalletApplication> list(Pageable pageable) {
        return applications.findAll(pageable);
    }

    @Transactional
    public WalletApplication complete(Long id, String currency) {
        WalletApplication app = get(id);
        if (app.getStatus() == WalletApplicationStatus.COMPLETED) {
            return app;
        }
        String ccy = currency == null || currency.isBlank() ? "USD" : currency.toUpperCase();
        walletSetupUseCase.createFull(app.getExtIdentifier(), ccy, null,
            app.getExtIdentifier(), app.getExtType());
        app.setStatus(WalletApplicationStatus.COMPLETED);
        return applications.save(app);
    }

    @Transactional
    public WalletApplication fail(Long id) {
        WalletApplication app = get(id);
        app.setStatus(WalletApplicationStatus.FAILED);
        app.setFailCounter((app.getFailCounter() == null ? 0 : app.getFailCounter()) + 1);
        return applications.save(app);
    }

    private static String sha256(String value) {
        try {
            byte[] dig = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(dig);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
