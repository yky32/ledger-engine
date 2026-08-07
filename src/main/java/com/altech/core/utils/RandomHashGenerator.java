package com.altech.core.utils;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class RandomHashGenerator {

    private static final String HASH_ALGORITHM = "SHA-256";
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    public static String generateRandomHash(int length) {
        try {
            // Generate a random salt
            byte[] salt = generateRandomSalt();

            // Concatenate the input string and salt
            byte[] inputBytes = (generateRandomString(5) + byteArrayToHexString(salt)).getBytes(StandardCharsets.UTF_8);

            // Create an instance of the SHA-256 algorithm
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);

            // Compute the hash value of the input string and salt
            byte[] hash = digest.digest(inputBytes);

            // Truncate the hash to the desired length
            byte[] truncatedHash = new byte[length / 2];
            System.arraycopy(hash, 0, truncatedHash, 0, truncatedHash.length);

            // Convert the byte array to a hexadecimal string
            StringBuilder hexString = new StringBuilder();
            for (byte b : truncatedHash) {
                String hex = String.format("%02x", b);
                hexString.append(hex);
            }

            // Return the hash value
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String generateRandomHash(String input, int length) {
        try {
            // Generate a random salt
            byte[] salt = generateRandomSalt();

            // Concatenate the input string and salt
            byte[] inputBytes = (input + byteArrayToHexString(salt)).getBytes(StandardCharsets.UTF_8);

            // Create an instance of the SHA-256 algorithm
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);

            // Compute the hash value of the input string and salt
            byte[] hash = digest.digest(inputBytes);

            // Truncate the hash to the desired length
            byte[] truncatedHash = new byte[length / 2];
            System.arraycopy(hash, 0, truncatedHash, 0, truncatedHash.length);

            // Convert the byte array to a hexadecimal string
            StringBuilder hexString = new StringBuilder();
            for (byte b : truncatedHash) {
                String hex = String.format("%02x", b);
                hexString.append(hex);
            }

            // Return the hash value
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static byte[] generateRandomSalt() {
        byte[] salt = new byte[16];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(salt);
        return salt;
    }

    private static String byteArrayToHexString(byte[] array) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : array) {
            String hex = String.format("%02x", b);
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public static String generateRandomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        SecureRandom random = new SecureRandom();
        for (int i = 0; i < length; i++) {
            int randomIndex = random.nextInt(CHARACTERS.length());
            char randomChar = CHARACTERS.charAt(randomIndex);
            sb.append(randomChar);
        }
        return sb.toString();
    }


    public static String hash(String input) {
        return hash(input, HASH_ALGORITHM, 0);
    }

    public static String hash(String input, String algorithm) {
        return hash(input, algorithm, 0);
    }
    /**
     * Hashes the input string using the specified algorithm ("SHA-256" or "SHA-512")
     * and optionally truncates the hex result to a given length.
     *
     * @param input     The string to hash
     * @param algorithm "SHA-256" or "SHA-512" - [HASH_ALGORITHM]
     * @param length    The number of characters to return from the hex string (set to 0 or less to return full hash)
     * @return Hexadecimal hash string, possibly truncated
     */
    public static String hash(String input, String algorithm, int length) {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            byte[] encodedHash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            String hex = bytesToHex(encodedHash);
            if (length > 0 && length < hex.length()) {
                return hex.substring(0, length);
            }
            return hex;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("Unsupported algorithm: " + algorithm, e);
        }
    }

    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            hexString.append(String.format("%02x", b));
        }
        return hexString.toString();
    }

    /**
     * Generates a crypto-style random address (like Ethereum, TRON, Solana).
     * Examples:
     *   - 0x9f8afee433eb0d17d044a2c57395fc32   (40 hex chars, lowercase)
     *   - 0x9F8AfEe433EB0D17D044A2C57395FC32   (uppercase version)
     *   - TJd9k8f3aB2cX7mN9pQ2vR5tY6uZ8wA1cE    (Base58 style, no 0OIl chars – like TRON/Solana)
     *
     * @param style Choose style: "eth" (0x + 40 hex), "eth-upper", "tron" (Base58 ~44 chars)
     * @return Crypto-looking random address
     */
    public static String generateCryptoLikeHash(String style) {
        return switch (style.toLowerCase()) {
            case "eth" -> "0x" + generateSecureHex(40);                    // Ethereum style (lowercase)
            case "eth-upper" -> "0x" + generateSecureHex(40).toUpperCase(); // Ethereum uppercase
            case "tron", "solana", "base58" -> generateBase58(32);         // TRON/Solana style (32 bytes → ~44 chars)
            default -> "0x" + generateSecureHex(40); // default = Ethereum lowercase
        };
    }

    public static String generateCryptoLikeHash() {
        return generateCryptoLikeHash("default");
    }

    /**
     * Internal: Generate cryptographically secure hex string of exact length (in chars)
     */
    private static String generateSecureHex(int hexLength) {
        byte[] randomBytes = new byte[hexLength / 2];
        new SecureRandom().nextBytes(randomBytes);
        return bytesToHex(randomBytes);
    }

    /**
     * Internal: Generate Base58-encoded string (like Bitcoin/TRON/Solana addresses)
     * Avoids confusing characters: 0, O, I, l
     */
    private static String generateBase58(int byteLength) {
        byte[] bytes = new byte[byteLength];
        new SecureRandom().nextBytes(bytes);

        final String ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";
        StringBuilder result = new StringBuilder();
        BigInteger num = new BigInteger(1, bytes);

        while (num.compareTo(BigInteger.ZERO) > 0) {
            int remainder = num.mod(BigInteger.valueOf(58)).intValue();
            result.insert(0, ALPHABET.charAt(remainder));
            num = num.divide(BigInteger.valueOf(58));
        }

        // Preserve leading zeros (if any)
        for (byte b : bytes) {
            if (b != 0) break;
            result.insert(0, ALPHABET.charAt(0));
        }

        return result.toString();
    }
}
