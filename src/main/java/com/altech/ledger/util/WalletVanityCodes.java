package com.altech.ledger.util;

/**
 * Customer-facing vanity / premium display codes for wallets (e.g. lucky digits).
 * <p>
 * <b>Not</b> a system identity: never use as PK, FK, movement key, or ownerId.
 * Generation / pool / reassignment logic is intentionally deferred.
 */
public final class WalletVanityCodes {

    private WalletVanityCodes() {}

    /**
     * Placeholder — assign a vanity code at wallet create when caller did not supply one.
     * <p>
     * <b>TODO (product):</b> replace with real rules, e.g.
     * <ul>
     *   <li>lucky-digit pool (8888 / 6666) with hold &amp; fee</li>
     *   <li>checksum / prefix / region format</li>
     *   <li>collision check via {@code existsByVanityCode}</li>
     *   <li>reassign with cooldown (old code RETIRED)</li>
     * </ul>
     *
     * @param ownerId stable CRM key (context only; must not be returned as vanity)
     * @return provisional code, or {@code null} to leave unassigned until real generator ships
     */
    public static String generatePlaceholder(String ownerId) {
        // Leave unassigned by default — premium codes are a product decision, not random noise.
        // When implementing: return a unique display code and persist on Wallet.vanityCode.
        return null;
    }

    /**
     * Normalize user-supplied vanity (trim, upper-case). Does not validate uniqueness / format yet.
     *
     * @return normalized code, or {@code null} if blank
     */
    public static String normalize(String vanityCode) {
        if (vanityCode == null) {
            return null;
        }
        String t = vanityCode.trim();
        if (t.isEmpty()) {
            return null;
        }
        return t.toUpperCase();
    }

    /**
     * Resolve which vanity to store: explicit request wins; else placeholder generator.
     */
    public static String resolveForCreate(String requestedVanityCode, String ownerId) {
        String explicit = normalize(requestedVanityCode);
        if (explicit != null) {
            return explicit;
        }
        return generatePlaceholder(ownerId);
    }
}
