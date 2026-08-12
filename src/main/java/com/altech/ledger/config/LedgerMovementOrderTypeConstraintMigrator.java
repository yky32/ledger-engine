package com.altech.ledger.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Hibernate {@code ddl-auto=update} does not widen PG enum check constraints.
 * Ensures {@code ledger_movement.order_type} accepts HOLD / RELEASE.
 */
@Slf4j
@Component
@Order(50)
@RequiredArgsConstructor
public class LedgerMovementOrderTypeConstraintMigrator implements ApplicationRunner {
    private final JdbcTemplate jdbcTemplate;

    private static final String CONSTRAINT = "ledger_movement_order_type_check";

    private static final String[] ORDER_TYPES = {
        "PAYMENT_LINK", "WITHDRAWAL", "WALLET_TRANSFER", "DEPOSIT",
        "ADJUSTMENT", "ADJUSTMENT_REFUND", "ADJUSTMENT_TOTAL",
        "BANK_CHARGE", "HANDLING_CHARGE", "IN_WALLET_TRANSFER", "SWIFT_TRANSFER",
        "EARN", "BURN", "PROCESS", "CHARGE",
        "HOLD", "RELEASE"
    };

    @Override
    public void run(ApplicationArguments args) {
        try {
            Integer exists = jdbcTemplate.queryForObject(
                """
                    select count(*) from information_schema.table_constraints
                    where table_name = 'ledger_movement' and constraint_name = ?
                    """,
                Integer.class,
                CONSTRAINT
            );
            if (exists == null || exists == 0) {
                return;
            }
            // Always recreate with full enum list (idempotent)
            jdbcTemplate.execute("alter table ledger_movement drop constraint if exists " + CONSTRAINT);
            String inList = String.join("','", ORDER_TYPES);
            jdbcTemplate.execute(
                "alter table ledger_movement add constraint " + CONSTRAINT
                    + " check (order_type in ('" + inList + "'))"
            );
            log.info("Refreshed {} to include HOLD/RELEASE", CONSTRAINT);
        } catch (Exception ex) {
            log.warn("Could not refresh {}: {} (non-fatal if not Postgres)", CONSTRAINT, ex.getMessage());
        }
    }
}
