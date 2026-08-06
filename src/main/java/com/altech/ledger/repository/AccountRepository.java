package com.altech.ledger.repository;

import com.altech.ledger.entity.po.ledger.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
    boolean existsByFullNumber(String fullNumber);

    Optional<Account> findByFullNumber(String fullNumber);

    Optional<Account> findByMainAccountAndSubAccount(String mainAccount, String subAccount);

    List<Account> findAllByMainAccount(String mainAccount);

    Optional<Account> findByMainAccountAndCurrency(String mainAccount, String currency);

    @Query("select a.mainAccount from Account a where a.mainAccount is not null")
    List<String> allMainAccountNumbers();

    @Query("select a.subAccount from Account a where a.mainAccount = :mainAccount")
    List<String> allSubAccountNumbers(@Param("mainAccount") String mainAccount);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.id in :ids order by a.id")
    List<Account> lockAllById(@Param("ids") List<Long> ids);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.id = :id")
    Optional<Account> lockById(@Param("id") Long id);
}
