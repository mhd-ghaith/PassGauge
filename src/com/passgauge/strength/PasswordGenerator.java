package com.passgauge.strength;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Generates a random password entirely on-device using {@link SecureRandom}.
 * Nothing here touches the network or disk.
 */
public final class PasswordGenerator {

    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String DIGITS = "0123456789";
    private static final String SYMBOLS = "!@#$%^&*()-_=+";

    private final SecureRandom random = new SecureRandom();

    /** Generates a password of the given length containing at least one of each character class. */
    public String generate(int length) {
        length = Math.max(4, length);
        List<Character> chars = new ArrayList<>(length);

        // guarantee at least one of each class
        chars.add(pick(LOWER));
        chars.add(pick(UPPER));
        chars.add(pick(DIGITS));
        chars.add(pick(SYMBOLS));

        String all = LOWER + UPPER + DIGITS + SYMBOLS;
        while (chars.size() < length) {
            chars.add(pick(all));
        }

        Collections.shuffle(chars, random);
        StringBuilder sb = new StringBuilder(length);
        for (char c : chars) sb.append(c);
        return sb.toString();
    }

    private char pick(String pool) {
        return pool.charAt(random.nextInt(pool.length()));
    }
}
