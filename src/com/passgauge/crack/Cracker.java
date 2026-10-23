package com.passgauge.crack;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Common contract for the sequential and parallel password crackers.
 * Both brute-force over {@code charset}, trying every string of length
 * 1..maxLength (shortest first) until one hashes to {@code targetHash}.
 */
public interface Cracker {

    /**
     * @param targetHash  SHA-256 hash (hex) of the password being searched for
     * @param charset     characters to draw candidates from
     * @param maxLength   longest candidate length to try
     * @param threadCount worker thread count (ignored by the sequential cracker)
     * @param hashCounter incremented on every hash attempt so the UI can show
     *                    a live hash rate while the search runs
     */
    CrackResult crack(String targetHash, char[] charset, int maxLength,
                       int threadCount, AtomicLong hashCounter) throws InterruptedException;
}
