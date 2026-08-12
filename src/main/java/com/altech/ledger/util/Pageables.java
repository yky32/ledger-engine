package com.altech.ledger.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;

/**
 * tgt.profile-style pagination helpers.
 * <p>
 * API page is <b>1-based</b> ({@code @PageableDefault(page = 1)}).
 * Convert with {@link #toZeroBased(Pageable)} before Spring Data repository calls.
 * Date filters are wall-clock strings: ISO-8601 instant <em>or</em> {@code yyyy-MM-dd}.
 */
public final class Pageables {
    public static final Instant EARLIEST = Instant.EPOCH;
    public static final Instant FAR_FUTURE = Instant.parse("9999-12-31T23:59:59Z");

    private Pageables() {}

    /**
     * Convert 1-based request pageable → 0-based {@link PageRequest} for JPA.
     * Preserves sort; empty sort defaults to {@code createDt DESC}.
     */
    public static PageRequest toZeroBased(Pageable pageable) {
        if (pageable == null || pageable.isUnpaged()) {
            return PageRequest.of(0, Integer.MAX_VALUE, Sort.by(Sort.Direction.DESC, "createDt"));
        }
        int page = Math.max(0, pageable.getPageNumber() - 1);
        int size = pageable.getPageSize() <= 0 ? Integer.MAX_VALUE : pageable.getPageSize();
        // guard absurd sizes for DB drivers that choke on Integer.MAX_VALUE
        if (size == Integer.MAX_VALUE) {
            size = 10_000;
        }
        Sort sort = pageable.getSort().isSorted()
            ? pageable.getSort()
            : Sort.by(Sort.Direction.DESC, "createDt");
        return PageRequest.of(page, size, sort);
    }

    public static Instant parseStartDt(String startDt) {
        if (startDt == null || startDt.isBlank()) {
            return EARLIEST;
        }
        return _parse(startDt.trim(), true);
    }

    public static Instant parseEndDt(String endDt) {
        if (endDt == null || endDt.isBlank()) {
            return FAR_FUTURE;
        }
        return _parse(endDt.trim(), false);
    }

    private static Instant _parse(String raw, boolean startOfDayIfDate) {
        try {
            return Instant.parse(raw);
        } catch (DateTimeParseException ignored) {
            // fall through
        }
        LocalDate d = LocalDate.parse(raw);
        if (startOfDayIfDate) {
            return d.atStartOfDay(ZoneOffset.UTC).toInstant();
        }
        return d.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().minusMillis(1);
    }
}
