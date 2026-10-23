package com.passgauge.strength;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Lightweight, dependency-free password strength analyzer modeled on the
 * ideas behind zxcvbn (length, character variety, dictionary/common-password
 * matching, sequential and repeated patterns, simple l33t-speak awareness),
 * rather than a port of the library itself. See the project README for why.
 *
 * This class never logs, stores, or otherwise retains the password it is
 * given \u2014 it reads it once, computes a result, and returns. No field in
 * this class holds a copy of any password analyzed.
 */
public final class StrengthAnalyzer {

    // A small built-in sample of extremely common passwords/words. Not
    // exhaustive \u2014 this is a demonstration list, not a breach-data corpus.
    private static final Set<String> COMMON_PASSWORDS = new HashSet<>(Arrays.asList(
            "password", "123456", "123456789", "12345678", "12345", "1234567",
            "qwerty", "qwerty123", "111111", "123123", "abc123", "password1",
            "iloveyou", "admin", "welcome", "monkey", "login", "letmein",
            "starwars", "dragon", "football", "baseball", "sunshine", "princess",
            "flower", "hottie", "loveme", "zaq1zaq1", "trustno1", "freedom",
            "whatever", "qazwsx", "michael", "superman", "batman", "shadow",
            "master", "jennifer", "jordan", "hunter", "letme1n", "passw0rd",
            "p@ssword", "p@ssw0rd", "changeme", "default", "guest", "root",
            "test", "user", "temp", "asdfgh", "asdf1234", "1q2w3e4r", "1qaz2wsx"
    ));

    private static final String[] KEYBOARD_RUNS = {
            "qwertyuiop", "asdfghjkl", "zxcvbnm", "1234567890", "qazwsx", "qweasdzxc"
    };

    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String DIGITS = "0123456789";
    private static final String SYMBOLS = "!@#$%^&*()-_=+[]{};:,.<>/?";

    public StrengthResult analyze(String password) {
        List<String> feedback = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();

        if (password == null || password.isEmpty()) {
            return new StrengthResult(0, 0,
                    List.of("No password entered yet."),
                    List.of("Start typing to see live feedback."),
                    "Enter a password above to see its strength.");
        }

        int length = password.length();
        boolean hasLower = password.chars().anyMatch(Character::isLowerCase);
        boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        boolean hasSymbol = password.chars().anyMatch(c -> !Character.isLetterOrDigit(c));

        int poolSize = 0;
        if (hasLower) poolSize += 26;
        if (hasUpper) poolSize += 26;
        if (hasDigit) poolSize += 10;
        if (hasSymbol) poolSize += SYMBOLS.length();
        if (poolSize == 0) poolSize = 26;

        double baseBits = length * (Math.log(poolSize) / Math.log(2));
        double penalty = 0;

        // --- Common password / dictionary-style check -------------------
        String lower = password.toLowerCase();
        String deLeeted = deLeet(lower);
        boolean isCommon = COMMON_PASSWORDS.contains(lower) || COMMON_PASSWORDS.contains(deLeeted)
                || COMMON_PASSWORDS.contains(lower.replaceAll("\\d+$", ""))
                || COMMON_PASSWORDS.contains(deLeeted.replaceAll("\\d+$", ""));
        if (isCommon) {
            feedback.add("This is one of the most commonly used passwords \u2014 attackers try these first.");
            suggestions.add("Avoid common words/passwords, even with a number or symbol tacked on.");
            penalty += 1000; // effectively forces score to 0
        }

        // --- Sequential run check (abc, 123, cba, 321) -------------------
        if (hasSequentialRun(lower, 3)) {
            feedback.add("Contains a predictable sequential pattern (like \"abc\" or \"123\").");
            suggestions.add("Avoid sequences of consecutive letters or digits.");
            penalty += 14;
        }

        // --- Keyboard-adjacency check -------------------------------------
        for (String run : KEYBOARD_RUNS) {
            if (containsRunOrReverse(lower, run, 4)) {
                feedback.add("Contains a keyboard-adjacent pattern (like \"qwerty\" or \"asdf\").");
                suggestions.add("Avoid characters that are next to each other on the keyboard.");
                penalty += 14;
                break;
            }
        }

        // --- Repeated character / repeated block check --------------------
        if (hasRepeatedChar(password, 3)) {
            feedback.add("Contains a character repeated several times in a row (like \"aaa\").");
            suggestions.add("Avoid repeating the same character multiple times.");
            penalty += 10;
        } else if (hasRepeatedBlock(password)) {
            feedback.add("Contains a short pattern repeated back-to-back (like \"abcabc\").");
            suggestions.add("Avoid repeating a short chunk of the password.");
            penalty += 8;
        }

        // --- Date-like pattern check ---------------------------------------
        if (password.matches(".*(19|20)\\d{2}.*") || password.matches(".*\\d{1,2}[/-]\\d{1,2}.*")) {
            feedback.add("Contains what looks like a date \u2014 dates are commonly guessed.");
            suggestions.add("Avoid birthdays, years, or other dates.");
            penalty += 6;
        }

        // --- Length checks ---------------------------------------------------
        if (length < 8) {
            feedback.add("Too short \u2014 short passwords are fast to brute-force.");
            suggestions.add("Use at least 12 characters; longer is always stronger.");
        } else if (length < 12) {
            suggestions.add("Consider lengthening it to 12+ characters for extra margin.");
        }

        // --- Variety checks ---------------------------------------------------
        int varietyCount = (hasLower ? 1 : 0) + (hasUpper ? 1 : 0) + (hasDigit ? 1 : 0) + (hasSymbol ? 1 : 0);
        if (varietyCount <= 1) {
            feedback.add("Uses only one type of character (e.g. just lowercase letters).");
        }
        if (!hasUpper) suggestions.add("Add an uppercase letter.");
        if (!hasDigit) suggestions.add("Add a digit.");
        if (!hasSymbol) suggestions.add("Add a symbol (e.g. ! @ # $).");

        double adjustedBits = Math.max(0, baseBits - penalty);

        int score;
        if (isCommon) {
            score = 0;
        } else if (adjustedBits < 28) {
            score = 0;
        } else if (adjustedBits < 36) {
            score = 1;
        } else if (adjustedBits < 60) {
            score = 2;
        } else if (adjustedBits < 80) {
            score = 3;
        } else {
            score = 4;
        }

        if (feedback.isEmpty()) {
            feedback.add("No obvious weaknesses detected in this estimate.");
        }
        if (suggestions.isEmpty()) {
            suggestions.add("This looks solid \u2014 keep it unique to this account and consider a password manager.");
        }

        String explanation = buildExplanation(score, length, varietyCount, isCommon, adjustedBits);

        return new StrengthResult(score, adjustedBits, feedback, suggestions, explanation);
    }

    private String buildExplanation(int score, int length, int varietyCount, boolean isCommon, double bits) {
        String label = StrengthResult.LABELS[score];
        if (isCommon) {
            return "Rated \"" + label + "\" because this password (or a close variant of it) appears on common "
                    + "password lists \u2014 length and symbols don't help if the whole password is already guessable.";
        }
        return String.format(
                "Rated \"%s\" based on its length (%d characters), use of %d character type%s, and an estimated "
                        + "search space of roughly %.0f bits after accounting for any predictable patterns found.",
                label, length, varietyCount, varietyCount == 1 ? "" : "s", bits);
    }

    // ---------------------------------------------------------------------
    // Pattern-detection helpers
    // ---------------------------------------------------------------------

    private boolean hasSequentialRun(String s, int runLength) {
        for (int i = 0; i + runLength <= s.length(); i++) {
            boolean ascending = true, descending = true;
            for (int j = 1; j < runLength; j++) {
                int diff = s.charAt(i + j) - s.charAt(i + j - 1);
                if (diff != 1) ascending = false;
                if (diff != -1) descending = false;
            }
            if (ascending || descending) return true;
        }
        return false;
    }

    private boolean containsRunOrReverse(String s, String run, int minLen) {
        for (int len = run.length(); len >= minLen; len--) {
            for (int i = 0; i + len <= run.length(); i++) {
                String chunk = run.substring(i, i + len);
                if (s.contains(chunk) || s.contains(new StringBuilder(chunk).reverse().toString())) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasRepeatedChar(String s, int runLength) {
        int count = 1;
        for (int i = 1; i < s.length(); i++) {
            if (s.charAt(i) == s.charAt(i - 1)) {
                count++;
                if (count >= runLength) return true;
            } else {
                count = 1;
            }
        }
        return false;
    }

    private boolean hasRepeatedBlock(String s) {
        int n = s.length();
        for (int blockLen = 2; blockLen <= n / 2; blockLen++) {
            for (int start = 0; start + blockLen * 2 <= n; start++) {
                String a = s.substring(start, start + blockLen);
                String b = s.substring(start + blockLen, start + blockLen * 2);
                if (a.equals(b)) return true;
            }
        }
        return false;
    }

    private String deLeet(String s) {
        return s.replace('0', 'o').replace('1', 'l').replace('3', 'e')
                .replace('4', 'a').replace('5', 's').replace('7', 't')
                .replace('@', 'a').replace('$', 's');
    }
}
