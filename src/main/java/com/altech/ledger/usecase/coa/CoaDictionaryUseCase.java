package com.altech.ledger.usecase.coa;

import com.altech.core.exception.BizException;
import com.altech.ledger.entity.dto.request.CreateCoaDictionaryRequestDto;
import com.altech.ledger.entity.dto.request.UpdateCoaDictionaryRequestDto;
import com.altech.ledger.entity.dto.response.GetCoaDictionaryResponseDto;
import com.altech.ledger.entity.enu.CoaDictionaryKind;
import com.altech.ledger.entity.po.coa.CoaDictionary;
import com.altech.ledger.exception.response.CoaErrorResponse;
import com.altech.ledger.repository.CoaDictionaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/**
 * Operator dictionary for COA digit segments and stems (01-02 = house operating).
 * Profiles stay the live chart; these rows are the definitions.
 */
@Component
@RequiredArgsConstructor
public class CoaDictionaryUseCase {
    private final CoaDictionaryRepository coaDictionaryRepository;

    private record Seed(
        CoaDictionaryKind kind,
        String code,
        String name,
        String side,
        String example,
        String definition
    ) {}

    private static final List<Seed> UA = List.of(
        new Seed(CoaDictionaryKind.ENTITY, "01", "Credit Card", "BOTH", "01-…",
            "UA product entity for credit-card books. Customer and house CC paths start with 01."),
        new Seed(CoaDictionaryKind.ENTITY, "02", "Loan", "BOTH", "02-…",
            "UA product entity for loan books. LN_TXN opens loan custodian 02-01-01."),
        new Seed(CoaDictionaryKind.TYPE, "01", "Custodian", "CUSTOMER", "xx-01-…",
            "Customer / member liability. Member books live under type 01 (01-01-01)."),
        new Seed(CoaDictionaryKind.TYPE, "02", "Operating", "HOUSE", "01-02-01",
            "House operating. Same-currency earn DR hits HOUSE 01-02-01."),
        new Seed(CoaDictionaryKind.TYPE, "04", "Expense", "HOUSE", "01-04-02",
            "House expense (corporate). Chart exists; earn sequences today DR operating 01-02, not 01-04."),
        new Seed(CoaDictionaryKind.SUB_TYPE, "01", "Individual / operating leaf", "BOTH", "01-01-01 · 01-02-01",
            "Under custodian: individual customer. Under operating: house operating leaf."),
        new Seed(CoaDictionaryKind.SUB_TYPE, "02", "Corporate", "HOUSE", "01-04-02",
            "House expense corporate leaf."),
        new Seed(CoaDictionaryKind.BUFFER, "00", "None", "BOTH", "…-00-HKD",
            "No buffer split. Always 00 on UA customer and house books."),
        new Seed(CoaDictionaryKind.STEM, "01-01", "Customer custodian", "CUSTOMER", "01-01-01-main-00-ccy",
            "Member wallet books. HKD = realtime cashback, LP = loyalty. mainAccount = card/loan product key."),
        new Seed(CoaDictionaryKind.STEM, "01-02", "House operating", "HOUSE", "01-02-01-9999-00-ccy",
            "UAF finance operating. Earn double-entry: DR 01-02-01 / CR 01-01-01, same currency. House mainAccount 9999."),
        new Seed(CoaDictionaryKind.STEM, "01-04", "House expense", "HOUSE", "01-04-02-9999-00-ccy",
            "UAF finance expense. Present on the HOUSE wallet; not posted by default earn sequences."),
        new Seed(CoaDictionaryKind.STEM, "02-01", "Loan custodian", "CUSTOMER", "02-01-01-main-00-HKD",
            "Loan customer books. Opened when LN_TXN arrives (not on CC ingest)."),
        new Seed(CoaDictionaryKind.PATH, "01-01-01", "Customer CC custodian individual", "CUSTOMER",
            "01-01-01-{mainAccount}-00-HKD|LP",
            "Customer credit-card custodian. One wallet per ownerId; HKD + LP books share event.mainAccount."),
        new Seed(CoaDictionaryKind.PATH, "01-02-01", "House CC operating", "HOUSE",
            "01-02-01-9999-00-HKD|LP",
            "Company operating book used as the earn counterparty (DEBIT)."),
        new Seed(CoaDictionaryKind.PATH, "01-04-02", "House CC expense corporate", "HOUSE",
            "01-04-02-9999-00-HKD|LP",
            "Company expense book on the HOUSE wallet."),
        new Seed(CoaDictionaryKind.PATH, "02-01-01", "Loan custodian individual", "CUSTOMER",
            "02-01-01-{mainAccount}-00-HKD",
            "Customer loan custodian. Same ownerId wallet as CC books; different entity 02.")
    );

    @Transactional
    public List<GetCoaDictionaryResponseDto> list() {
        ensureSeed();
        return coaDictionaryRepository.findAllByIsActiveTrueOrderByKindAscCodeAsc().stream()
            .map(this::toDto)
            .toList();
    }

    @Transactional
    public List<GetCoaDictionaryResponseDto> ensure() {
        ensureSeed();
        return list();
    }

    @Transactional(readOnly = true)
    public GetCoaDictionaryResponseDto get(Long id) {
        return toDto(_require(id));
    }

    @Transactional
    public GetCoaDictionaryResponseDto create(CreateCoaDictionaryRequestDto req) {
        CoaDictionaryKind kind = parseKind(req.kind());
        String code = normalizeCode(req.code());
        if (coaDictionaryRepository.existsByKindAndCode(kind, code)) {
            throw new BizException(CoaErrorResponse.COA0409, "kind=" + kind + " code=" + code);
        }
        CoaDictionary row = new CoaDictionary();
        row.setKind(kind);
        row.setCode(code);
        row.setName(blankToNull(req.name()));
        row.setDefinition(blankToNull(req.definition()));
        row.setExample(blankToNull(req.example()));
        row.setSide(blankToNull(req.side()));
        row.setIsActive(true);
        return toDto(coaDictionaryRepository.save(row));
    }

    @Transactional
    public GetCoaDictionaryResponseDto update(Long id, UpdateCoaDictionaryRequestDto req) {
        CoaDictionary row = _require(id);
        if (req.kind() != null && !req.kind().isBlank()) {
            row.setKind(parseKind(req.kind()));
        }
        if (req.code() != null && !req.code().isBlank()) {
            row.setCode(normalizeCode(req.code()));
        }
        if (coaDictionaryRepository.findByKindAndCode(row.getKind(), row.getCode())
            .filter(other -> !other.getId().equals(id))
            .isPresent()) {
            throw new BizException(CoaErrorResponse.COA0409,
                "kind=" + row.getKind() + " code=" + row.getCode());
        }
        if (req.name() != null) {
            row.setName(blankToNull(req.name()));
        }
        if (req.definition() != null) {
            row.setDefinition(blankToNull(req.definition()));
        }
        if (req.example() != null) {
            row.setExample(blankToNull(req.example()));
        }
        if (req.side() != null) {
            row.setSide(blankToNull(req.side()));
        }
        return toDto(coaDictionaryRepository.save(row));
    }

    @Transactional
    public void delete(Long id) {
        CoaDictionary row = _require(id);
        coaDictionaryRepository.delete(row);
    }

    void ensureSeed() {
        for (Seed s : UA) {
            coaDictionaryRepository.findByKindAndCode(s.kind(), s.code()).ifPresentOrElse(existing -> {
                boolean dirty = false;
                if (isBlank(existing.getName()) && s.name() != null) {
                    existing.setName(s.name());
                    dirty = true;
                }
                if (isBlank(existing.getDefinition()) && s.definition() != null) {
                    existing.setDefinition(s.definition());
                    dirty = true;
                }
                if (isBlank(existing.getExample()) && s.example() != null) {
                    existing.setExample(s.example());
                    dirty = true;
                }
                if (isBlank(existing.getSide()) && s.side() != null) {
                    existing.setSide(s.side());
                    dirty = true;
                }
                if (dirty) {
                    coaDictionaryRepository.save(existing);
                }
            }, () -> {
                CoaDictionary row = new CoaDictionary();
                row.setKind(s.kind());
                row.setCode(s.code());
                row.setName(s.name());
                row.setDefinition(s.definition());
                row.setExample(s.example());
                row.setSide(s.side());
                row.setIsActive(true);
                coaDictionaryRepository.save(row);
            });
        }
    }

    private CoaDictionary _require(Long id) {
        return coaDictionaryRepository.findById(id)
            .orElseThrow(() -> new BizException(CoaErrorResponse.COA0404, "id=" + id));
    }

    private static CoaDictionaryKind parseKind(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BizException(CoaErrorResponse.COA0400, "kind required");
        }
        try {
            return CoaDictionaryKind.valueOf(raw.trim().toUpperCase(Locale.ROOT).replace('-', '_'));
        } catch (IllegalArgumentException ex) {
            throw new BizException(CoaErrorResponse.COA0400, "kind=" + raw);
        }
    }

    private static String normalizeCode(String code) {
        return code.trim().replace(' ', '-');
    }

    private static String blankToNull(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return s.trim();
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private GetCoaDictionaryResponseDto toDto(CoaDictionary r) {
        return GetCoaDictionaryResponseDto.builder()
            .id(r.getId())
            .kind(r.getKind() == null ? null : r.getKind().name())
            .code(r.getCode())
            .name(r.getName())
            .definition(r.getDefinition())
            .example(r.getExample())
            .side(r.getSide())
            .createDt(r.getCreateDt())
            .updateDt(r.getUpdateDt())
            .build();
    }
}
