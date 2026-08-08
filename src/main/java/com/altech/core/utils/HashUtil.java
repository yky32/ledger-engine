package com.altech.core.utils;

import com.altech.core.exception.BizException;
import com.altech.core.response.SystemResponse;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Deterministic digests (hex lowercase). Aligns with PG payment-service / tech-core.
 * <p>
 * Prefer {@link #SHA256(String)} / {@link #SHA512(String)} for fingerprints (e.g. PAN identity).
 * For opaque random public ids use {@link RandomHashGenerator#generateRandomHash(int)}.
 */
public final class HashUtil {

    private HashUtil() {}

    public static String hash(String input, String algorithm) {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            byte[] hashBytes = digest.digest(
                input == null ? new byte[0] : input.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder(hashBytes.length * 2);
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new BizException(SystemResponse.SYS9998, "Hash algorithm not found: " + algorithm);
        }
    }

    public static String SHA256(String input) {
        return hash(input, "SHA-256");
    }

    public static String SHA512(String input) {
        return hash(input, "SHA-512");
    }
}
