package com.altech.core.utils.generator.id;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

/**
 * Twitter-style snowflake primary keys (64-bit long).
 * <p>
 * Layout: 1 sign | 41 ms timestamp | 10 worker | 12 sequence.
 * Worker id from env {@code LEDGER_WORKER_ID} / {@code SNOWFLAKE_WORKER_ID} (0–1023), default 1.
 */
public class SnowflakeIdGenerator implements IdentifierGenerator {

    /** Custom epoch: 2024-01-01 UTC — keeps ids shorter for a while. */
    private static final long EPOCH_MS = 1_704_067_200_000L;

    private static final long WORKER_BITS = 10L;
    private static final long SEQUENCE_BITS = 12L;
    private static final long MAX_WORKER = (1L << WORKER_BITS) - 1L;
    private static final long MAX_SEQUENCE = (1L << SEQUENCE_BITS) - 1L;
    private static final long WORKER_SHIFT = SEQUENCE_BITS;
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_BITS;

    private static final long WORKER_ID = _resolveWorkerId();

    private static long lastTimestamp = -1L;
    private static long sequence = 0L;

    @Override
    public Object generate(SharedSessionContractImplementor session, Object object) throws HibernateException {
        return nextId();
    }

    public static synchronized long nextId() {
        long ts = System.currentTimeMillis();
        if (ts < lastTimestamp) {
            // clock moved backwards — wait until we catch up
            ts = _waitUntil(lastTimestamp);
        }
        if (ts == lastTimestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            if (sequence == 0L) {
                ts = _waitUntil(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }
        lastTimestamp = ts;
        return ((ts - EPOCH_MS) << TIMESTAMP_SHIFT)
            | (WORKER_ID << WORKER_SHIFT)
            | sequence;
    }

    private static long _waitUntil(long last) {
        long ts = System.currentTimeMillis();
        while (ts <= last) {
            ts = System.currentTimeMillis();
        }
        return ts;
    }

    private static long _resolveWorkerId() {
        String raw = System.getenv("LEDGER_WORKER_ID");
        if (raw == null || raw.isBlank()) {
            raw = System.getenv("SNOWFLAKE_WORKER_ID");
        }
        if (raw == null || raw.isBlank()) {
            raw = System.getProperty("ledger.worker-id", "1");
        }
        try {
            long id = Long.parseLong(raw.trim());
            if (id < 0 || id > MAX_WORKER) {
                throw new IllegalStateException("worker id out of range 0.." + MAX_WORKER + ": " + id);
            }
            return id;
        } catch (NumberFormatException ex) {
            return 1L;
        }
    }
}
