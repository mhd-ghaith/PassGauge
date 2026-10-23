package com.passgauge.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Small stateless helper around SHA-256 hashing and keyspace enumeration. */
public final class HashUtil {

    private HashUtil() {}

    /** Returns the SHA-256 hash of {@code input} as a lowercase hex string. */
    public static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed to be present on every standard JVM.
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    /**
     * Decodes {@code index} into the index-th candidate string of length
     * {@code length} over {@code charset}, treating the charset as the digits
     * of a mixed-radix number. This lets any worker thread jump straight to
     * any point in the keyspace without generating every string before it -
     * which is what makes splitting the search across threads possible.
     */
    public static String indexToCandidate(long index, char[] charset, int length) {
        char[] out = new char[length];
        int base = charset.length;
        for (int pos = length - 1; pos >= 0; pos--) {
            out[pos] = charset[(int) (index % base)];
            index /= base;
        }
        return new String(out);
    }
}
