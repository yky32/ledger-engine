package com.altech.ledger.usecase.coa;

import com.altech.core.constant.enu.Currency;
import com.altech.core.exception.BizException;
import com.altech.ledger.entity.dto.request.AccountOpenSpecDto;
import com.altech.ledger.entity.dto.request.CreateCoaProfileRequestDto;
import com.altech.ledger.entity.dto.request.CreateWalletOnboardRequestDto;
import com.altech.ledger.entity.dto.response.GetHouseBooksResponseDto;
import com.altech.ledger.entity.dto.response.GetWalletAccountResponseDto;
import com.altech.ledger.entity.po.coa.CoaProfile;
import com.altech.ledger.entity.po.ledger.Account;
import com.altech.ledger.entity.enu.WalletType;
import com.altech.ledger.entity.po.ledger.Wallet;
import com.altech.ledger.exception.response.AccountErrorResponse;
import com.altech.ledger.exception.response.CoaErrorResponse;
import com.altech.ledger.exception.response.WalletErrorResponse;
import com.altech.ledger.repository.AccountRepository;
import com.altech.ledger.repository.CoaProfileRepository;
import com.altech.ledger.repository.WalletRepository;
import com.altech.ledger.service.DtoWrapper;
import com.altech.ledger.usecase.wallet.CreateWalletOnboardingUseCase;
import com.altech.ledger.util.CoaCodes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Corporate COA → one company wallet → accounts.
 * Each product client (e.g. UAF) gets one house wallet; HOUSE_* rows share that {@code walletId}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HouseBooksUseCase {
    /** Company wallet for UAF finance. Not a member CUST. */
    public static final String DEFAULT_OWNER = "HOUSE";
    /** Pre-HOUSE ownerId — renamed in place on ensure. */
    public static final String LEGACY_OWNER = "PROGRAM";

    public static boolean isHouseOwner(String ownerId) {
        if (ownerId == null || ownerId.isBlank()) {
            return false;
        }
        String o = ownerId.trim().toUpperCase(Locale.ROOT);
        return DEFAULT_OWNER.equals(o) || LEGACY_OWNER.equals(o);
    }

    public static String canonicalOwner(String ownerId) {
        if (ownerId == null || ownerId.isBlank() || isHouseOwner(ownerId)) {
            return DEFAULT_OWNER;
        }
        return ownerId.trim();
    }

    private record HouseSeed(String code, String name, String entity, String type, String subType,
                             String buffer, String currency) {}

    /**
     * House chart. Operating matches UA movement example {@code 01-02-01} (not 01-02-02).
     * Expense stays 01-04-02.
     */
    private static final List<HouseSeed> UA_HOUSE = List.of(
        new HouseSeed("HOUSE_CC_OP_HKD", "CC Operating HKD", "01", "02", "01", "00", "HKD"),
        new HouseSeed("HOUSE_CC_OP_LP", "CC Operating LP", "01", "02", "01", "00", "LP"),
        new HouseSeed("HOUSE_CC_EXP_HKD", "CC Expense Corporate HKD", "01", "04", "02", "00", "HKD"),
        new HouseSeed("HOUSE_CC_EXP_LP", "CC Expense Corporate LP", "01", "04", "02", "00", "LP")
    );

    private final CoaProfileRepository coaProfileRepository;
    private final WalletRepository walletRepository;
    private final AccountRepository accountRepository;
    private final CreateWalletOnboardingUseCase createWalletOnboardingUseCase;
    private final CoaProfileUseCase coaProfileUseCase;

    /** Rename leftover PROGRAM → HOUSE. No-op if already HOUSE or neither exists. */
    @Transactional
    public void migrateLegacyOwner() {
        _findOrMigrateHouseWallet(DEFAULT_OWNER);
    }

    @Transactional(readOnly = true)
    public GetHouseBooksResponseDto get() {
        List<CoaProfile> house = _houseProfiles();
        Long walletId = house.stream().map(CoaProfile::getWalletId).filter(id -> id != null).findFirst().orElse(null);
        if (walletId == null) {
            return new GetHouseBooksResponseDto(null, DEFAULT_OWNER, _dtos(house), List.of());
        }
        Wallet wallet = walletRepository.findById(walletId).orElse(null);
        String ownerId = wallet != null ? wallet.getOwnerId() : DEFAULT_OWNER;
        List<GetWalletAccountResponseDto> books = wallet == null
            ? List.of()
            : accountRepository.findAllByWalletId(walletId).stream()
                .map(DtoWrapper::getWalletAccountResponseDto)
                .toList();
        return new GetHouseBooksResponseDto(walletId, ownerId, _dtos(house), books);
    }

    /** createIfNotFound UA house COA + the one company wallet + accounts. */
    @Transactional
    public GetHouseBooksResponseDto ensure(String ownerId) {
        _seedUaHouseProfiles();
        return assignWallet(ownerId);
    }

    @Transactional
    public GetHouseBooksResponseDto assignWallet(String ownerId) {
        String oid = canonicalOwner(ownerId);
        List<CoaProfile> house = _houseProfiles();
        if (house.isEmpty()) {
            _seedUaHouseProfiles();
            house = _houseProfiles();
        }
        if (house.isEmpty()) {
            throw new BizException(CoaErrorResponse.COA0400, "Create HOUSE_* COA profiles first");
        }
        Wallet wallet = _findOrMigrateHouseWallet(oid);
        if (wallet == null) {
            wallet = _openHouseWallet(oid, house);
        }
        if (wallet.getWalletType() != WalletType.CORPORATE) {
            wallet.setWalletType(WalletType.CORPORATE);
            wallet = walletRepository.save(wallet);
        }
        for (CoaProfile p : house) {
            p.setWalletId(wallet.getId());
            coaProfileRepository.save(p);
            _ensureBook(wallet, p);
        }
        List<GetWalletAccountResponseDto> books = accountRepository.findAllByWalletId(wallet.getId()).stream()
            .map(DtoWrapper::getWalletAccountResponseDto)
            .toList();
        log.info("House COA assigned walletId={} ownerId={} profiles={} accounts={}",
            wallet.getId(), oid, house.size(), books.size());
        return new GetHouseBooksResponseDto(wallet.getId(), oid, _dtos(house), books);
    }

    private Wallet _openHouseWallet(String ownerId, List<CoaProfile> house) {
        CoaProfile seed = house.stream()
            .filter(p -> "HOUSE_CC_OP_HKD".equalsIgnoreCase(p.getCode())
                || "HOUSE_CC_OP_LP".equalsIgnoreCase(p.getCode())
                || "HOUSE_LP".equalsIgnoreCase(p.getCode()))
            .findFirst()
            .orElse(house.get(0));
        Currency settlement = house.stream()
            .filter(p -> "HKD".equalsIgnoreCase(p.getCurrency()))
            .map(p -> Currency.get(p.getCurrency()))
            .findFirst()
            .orElse(Currency.HKD);
        Set<String> seen = new HashSet<>();
        seen.add(settlement.getIsoCode());
        List<AccountOpenSpecDto> extras = new ArrayList<>();
        for (CoaProfile p : house) {
            String iso = p.getCurrency() == null ? "LP" : p.getCurrency().trim().toUpperCase(Locale.ROOT);
            if (!seen.add(iso)) {
                continue;
            }
            extras.add(new AccountOpenSpecDto(
                iso,
                p.getName(),
                false,
                Boolean.TRUE.equals(p.getPoolAllowNegative()),
                Currency.get(iso)));
        }
        try {
            createWalletOnboardingUseCase.execute(new CreateWalletOnboardRequestDto(
                ownerId,
                settlement,
                DEFAULT_OWNER.equals(ownerId) ? "UAF HOUSE" : "System " + ownerId,
                null,
                seed.getCode(),
                extras,
                CoaCodes.HOUSE_MAIN_ACCOUNT));
        } catch (BizException ex) {
            String code = ex.getResponse() != null ? ex.getResponse().getCode() : null;
            if (!WalletErrorResponse.WAL0409.getCode().equals(code)) {
                throw ex;
            }
        }
        return walletRepository.findByOwnerId(ownerId)
            .orElseThrow(() -> new BizException(AccountErrorResponse.ACC0404,
                "House wallet missing after onboard ownerId=" + ownerId));
    }

    private void _ensureBook(Wallet wallet, CoaProfile coa) {
        if (wallet.getAccountId() == null) {
            throw new BizException(AccountErrorResponse.ACC0404, "House wallet primary missing");
        }
        Currency ccy = Currency.get(coa.getCurrency() == null ? "LP" : coa.getCurrency());
        Account primary = accountRepository.findById(wallet.getAccountId())
            .orElseThrow(() -> new BizException(AccountErrorResponse.ACC0404,
                "Primary account missing for wallet " + wallet.getId()));
        String entity = blank(coa.getEntity(), CoaCodes.ENTITY);
        String type = blank(coa.getType(), CoaCodes.typeCodeLiability());
        String subType = blank(coa.getSubType(), CoaCodes.SUB_TYPE);
        Optional<Account> existing = accountRepository
            .findFirstByMainAccountAndEntityAndTypeAndSubTypeAndCurrency(
                primary.getMainAccount(), entity, type, subType, ccy);
        if (existing.isPresent()) {
            Account a = existing.get();
            boolean dirty = false;
            if (a.getWalletId() == null) {
                a.setWalletId(wallet.getId());
                dirty = true;
            }
            if (Boolean.TRUE.equals(coa.getPoolAllowNegative()) && !a.isAllowNegative()) {
                a.setAllowNegative(true);
                dirty = true;
            }
            if (dirty) {
                accountRepository.save(a);
            }
            return;
        }
        String buffer = blank(coa.getBuffer(), CoaCodes.BUFFER);
        String fullNumber = CoaCodes.fullNumber(
            entity, type, subType, primary.getMainAccount(), buffer, ccy);
        accountRepository.save(Account.builder()
            .walletId(wallet.getId())
            .fullNumber(fullNumber)
            .entity(entity)
            .type(type)
            .subType(subType)
            .mainAccount(primary.getMainAccount())
            .buffer(buffer)
            .currency(ccy)
            .allowNegative(Boolean.TRUE.equals(coa.getPoolAllowNegative()))
            .build());
    }

    /** HOUSE first; rename leftover PROGRAM row in place (same walletId). */
    private Wallet _findOrMigrateHouseWallet(String oid) {
        Optional<Wallet> hit = walletRepository.findByOwnerId(oid);
        if (hit.isPresent()) {
            return hit.get();
        }
        if (!DEFAULT_OWNER.equals(oid)) {
            return null;
        }
        Optional<Wallet> legacy = walletRepository.findByOwnerId(LEGACY_OWNER);
        if (legacy.isEmpty()) {
            return null;
        }
        Wallet w = legacy.get();
        w.setOwnerId(DEFAULT_OWNER);
        w.setName("UAF HOUSE");
        if (w.getVanityCode() != null && LEGACY_OWNER.equalsIgnoreCase(w.getVanityCode())) {
            w.setVanityCode(DEFAULT_OWNER);
        }
        w.setWalletType(WalletType.CORPORATE);
        w = walletRepository.save(w);
        log.info("Renamed house wallet ownerId {} → {} walletId={}", LEGACY_OWNER, DEFAULT_OWNER, w.getId());
        return w;
    }

    private void _seedUaHouseProfiles() {
        for (HouseSeed s : UA_HOUSE) {
            coaProfileRepository.findByCode(s.code()).ifPresentOrElse(existing -> {
                boolean dirty = false;
                if (!s.entity().equals(existing.getEntity())) {
                    existing.setEntity(s.entity());
                    dirty = true;
                }
                if (!s.type().equals(existing.getType())) {
                    existing.setType(s.type());
                    dirty = true;
                }
                if (!s.subType().equals(existing.getSubType())) {
                    existing.setSubType(s.subType());
                    dirty = true;
                }
                if (!s.buffer().equals(existing.getBuffer())) {
                    existing.setBuffer(s.buffer());
                    dirty = true;
                }
                if (!s.currency().equalsIgnoreCase(existing.getCurrency())) {
                    existing.setCurrency(s.currency());
                    dirty = true;
                }
                if (!Boolean.TRUE.equals(existing.getPoolAllowNegative())) {
                    existing.setPoolAllowNegative(true);
                    dirty = true;
                }
                if (dirty) {
                    existing.setName(s.name());
                    coaProfileRepository.save(existing);
                    log.info("Aligned house COA {} to {}-{}-{} {}",
                        s.code(), s.entity(), s.type(), s.subType(), s.currency());
                }
            }, () -> coaProfileUseCase.create(new CreateCoaProfileRequestDto(
                s.code(), s.name(), null, false, true,
                s.entity(), s.type(), s.subType(), s.buffer(), s.currency(), true, null)));
        }
    }

    private List<CoaProfile> _houseProfiles() {
        return coaProfileRepository.findAllByIsActiveTrueOrderByCodeAsc().stream()
            .filter(p -> CoaCodes.isHouseCode(p.getCode()))
            .toList();
    }

    private List<com.altech.ledger.entity.dto.response.GetCoaProfileResponseDto> _dtos(List<CoaProfile> house) {
        return house.stream().map(DtoWrapper::getCoaProfileResponseDto).toList();
    }

    private static String blank(String v, String d) {
        return v == null || v.isBlank() ? d : v.trim();
    }
}
