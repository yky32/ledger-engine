package com.altech.ledger.usecase.setup;

import com.altech.ledger.entity.dto.parity.ParityDtos.*;
import com.altech.ledger.entity.enu.RecipientStatus;
import com.altech.ledger.entity.po.Recipient;
import com.altech.ledger.exception.LedgerException;
import com.altech.ledger.repository.RecipientRepository;
import com.altech.ledger.service.DtoMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RecipientSetupUseCase {
    private final RecipientRepository recipients;

    public RecipientSetupUseCase(RecipientRepository recipients) {
        this.recipients = recipients;
    }

    @Transactional
    public RecipientResponse create(CreateRecipientRequest dto) {
        Recipient r = new Recipient(dto.transferChannel(),
            dto.status() == null ? RecipientStatus.ACTIVE : dto.status(), dto.metadata());
        if (dto.tenantId() != null) r.setTenantId(dto.tenantId());
        return DtoMapper.toRecipient(recipients.save(r));
    }

    @Transactional(readOnly = true)
    public RecipientResponse getOne(Long id) {
        return DtoMapper.toRecipient(recipients.findById(id)
            .orElseThrow(() -> LedgerException.notFound("Recipient not found: " + id)));
    }

    @Transactional(readOnly = true)
    public Page<RecipientResponse> getAll(Pageable pageable) {
        return recipients.findAll(pageable).map(DtoMapper::toRecipient);
    }

    @Transactional(readOnly = true)
    public List<RecipientResponse> myRecipients(Long tenantId) {
        return recipients.findByTenantId(tenantId).stream().map(DtoMapper::toRecipient).toList();
    }

    @Transactional
    public RecipientResponse update(Long id, UpdateRecipientRequest dto) {
        Recipient r = recipients.findById(id)
            .orElseThrow(() -> LedgerException.notFound("Recipient not found: " + id));
        if (dto.status() != null) r.setStatus(dto.status());
        if (dto.transferChannel() != null) r.setTransferChannel(dto.transferChannel());
        if (dto.metadata() != null) r.setMetadata(dto.metadata());
        return DtoMapper.toRecipient(recipients.save(r));
    }

    @Transactional
    public void delete(Long id) {
        if (!recipients.existsById(id)) {
            throw LedgerException.notFound("Recipient not found: " + id);
        }
        recipients.deleteById(id);
    }
}
