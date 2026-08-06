package com.altech.ledger.usecase.account.virtual;

import com.altech.ledger.entity.dto.parity.ParityDtos.*;
import com.altech.ledger.entity.enu.VirtualAccountApplicationStatus;
import com.altech.ledger.entity.po.ledger.VirtualAccount;
import com.altech.ledger.entity.po.ledger.VirtualAccountApplication;
import com.altech.ledger.exception.LedgerException;
import com.altech.ledger.repository.VirtualAccountApplicationRepository;
import com.altech.ledger.service.DtoMapper;
import com.altech.ledger.service.VirtualAccountService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Port of the-wallet-ledger VirtualAccountApplicationUseCase.
 */
@Service
public class VirtualAccountApplicationUseCase {
    private final VirtualAccountApplicationRepository applications;
    private final VirtualAccountService virtualAccountService;

    public VirtualAccountApplicationUseCase(VirtualAccountApplicationRepository applications,
                                            VirtualAccountService virtualAccountService) {
        this.applications = applications;
        this.virtualAccountService = virtualAccountService;
    }

    @Transactional
    public VirtualAccountApplicationResponse apply(CreateVirtualAccountApplicationRequest dto) {
        VirtualAccountApplication app = new VirtualAccountApplication(
            dto.type(), dto.extIdentifier(), dto.extType());
        app.setRemark(dto.remark());
        app.setMetadata(dto.metadata());
        return DtoMapper.toVaApp(applications.save(app));
    }

    @Transactional(readOnly = true)
    public VirtualAccountApplicationResponse getApplication(Long id) {
        return DtoMapper.toVaApp(app(id));
    }

    @Transactional(readOnly = true)
    public Page<VirtualAccountApplicationResponse> listApplications(Pageable pageable) {
        return applications.findAll(pageable).map(DtoMapper::toVaApp);
    }

    @Transactional
    public VirtualAccountApplicationResponse patchStatus(Long id,
                                                         PatchVirtualAccountApplicationStatusRequest dto) {
        VirtualAccountApplication app = app(id);
        app.setStatus(dto.status());
        if (dto.remark() != null) app.setRemark(dto.remark());
        if (dto.status() == VirtualAccountApplicationStatus.APPROVED && app.getVirtualAccount() == null) {
            VirtualAccount va = virtualAccountService.createWithSubAccounts(
                app.getExtIdentifier(), app.getExtType(), app.getType(), "VA-" + id, null);
            app.setVirtualAccount(va);
        }
        return DtoMapper.toVaApp(applications.save(app));
    }

    @Transactional
    public VirtualAccountApplicationResponse patchMetadata(Long id,
                                                           PatchVirtualAccountApplicationMetadataRequest dto) {
        VirtualAccountApplication app = app(id);
        app.setMetadata(dto.metadata());
        return DtoMapper.toVaApp(applications.save(app));
    }

    private VirtualAccountApplication app(Long id) {
        return applications.findById(id)
            .orElseThrow(() -> LedgerException.notFound("VA application not found: " + id));
    }
}
