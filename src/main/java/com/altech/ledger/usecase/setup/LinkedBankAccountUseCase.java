package com.altech.ledger.usecase.setup;

import com.altech.ledger.entity.dto.parity.ParityDtos.*;
import com.altech.ledger.entity.enu.RecipientStatus;
import com.altech.ledger.entity.po.LinkedBankAccount;
import com.altech.ledger.exception.LedgerException;
import com.altech.ledger.repository.LinkedBankAccountRepository;
import com.altech.ledger.service.DtoMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class LinkedBankAccountUseCase {
    private final LinkedBankAccountRepository repo;

    public LinkedBankAccountUseCase(LinkedBankAccountRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public LinkedBankAccountResponse create(CreateLinkedBankAccountRequest dto) {
        LinkedBankAccount a = new LinkedBankAccount(
            dto.status() == null ? RecipientStatus.ACTIVE : dto.status(), dto.metadata());
        if (dto.tenantId() != null) a.setTenantId(dto.tenantId());
        return DtoMapper.toLinkedBank(repo.save(a));
    }

    @Transactional(readOnly = true)
    public LinkedBankAccountResponse getById(Long id) {
        return DtoMapper.toLinkedBank(repo.findById(id)
            .orElseThrow(() -> LedgerException.notFound("Linked bank account not found: " + id)));
    }

    @Transactional(readOnly = true)
    public Optional<LinkedBankAccount> getOptionalById(Long id) {
        return repo.findById(id);
    }

    @Transactional
    public LinkedBankAccountResponse update(Long id, UpdateLinkedBankAccountRequest dto) {
        LinkedBankAccount a = repo.findById(id)
            .orElseThrow(() -> LedgerException.notFound("Linked bank account not found: " + id));
        if (dto.status() != null) a.setStatus(dto.status());
        if (dto.metadata() != null) a.setMetadata(dto.metadata());
        return DtoMapper.toLinkedBank(repo.save(a));
    }

    @Transactional(readOnly = true)
    public List<LinkedBankAccountResponse> getByTenant(Long tenantId) {
        return repo.findByTenantId(tenantId).stream().map(DtoMapper::toLinkedBank).toList();
    }

    @Transactional(readOnly = true)
    public List<LinkedBankAccountResponse> getMyAccount(Long tenantId) {
        return getByTenant(tenantId);
    }
}
