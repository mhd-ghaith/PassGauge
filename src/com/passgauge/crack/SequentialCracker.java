package com.passgauge.crack;

import com.passgauge.util.HashUtil;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Single-threaded baseline. One loop, one thread, no shared mutable state -
 * this is the control case the parallel version is measured against.
 */
public class SequentialCracker implements Cracker {

    @Override
    public CrackResult crack(String targetHash, char[] charset, int maxLength,
                              int threadCount, AtomicLong hashCounter) {
        long startTime = System.nanoTime();
        long hashes = 0;

        for (int length = 1; length <= maxLength; length++) {
            long spaceSize = (long) Math.pow(charset.length, length);
            for (long index = 0; index < spaceSize; index++) {
                String candidate = HashUtil.indexToCandidate(index, charset, length);
                hashes++;
                hashCounter.incrementAndGet();
                if (HashUtil.sha256(candidate).equals(targetHash)) {
                    return new CrackResult(true, candidate, System.nanoTime() - startTime, hashes, 1);
                }
            }
        }
        return new CrackResult(false, null, System.nanoTime() - startTime, hashes, 1);
    }
}
