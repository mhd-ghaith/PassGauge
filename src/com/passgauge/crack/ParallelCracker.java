package com.passgauge.crack;

import com.passgauge.util.HashUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Multi-threaded cracker. For each candidate length, the keyspace
 * [0, charset.length^length) is split into {@code threadCount} contiguous
 * ranges and each range is searched by its own worker (data parallelism;
 * HashUtil.indexToCandidate lets a worker jump straight into the middle of
 * the keyspace without generating everything before it).
 *
 * Synchronization notes (for the report):
 *  - {@code found} (AtomicBoolean) is the only shared mutable state every
 *    worker touches. It only ever flips false -> true via compareAndSet, so
 *    there is no race on "who found the match first" and no lost update.
 *  - {@code hashCounter} (AtomicLong) accumulates the live hash-rate shown
 *    by the UI. incrementAndGet() is lock-free, so it adds negligible
 *    contention even at millions of calls/sec.
 *  - Workers never wait on each other - no locks are taken, so there is no
 *    possibility of deadlock. Each worker only polls {@code found} between
 *    iterations and stops as soon as it flips to true.
 *  - {@code ExecutorService.invokeAll(...)} blocks the calling thread until
 *    every worker for the current length has finished or stopped, which is
 *    what keeps lengths processed in order without extra coordination code.
 */
public class ParallelCracker implements Cracker {

    @Override
    public CrackResult crack(String targetHash, char[] charset, int maxLength,
                              int threadCount, AtomicLong hashCounter) throws InterruptedException {
        long startTime = System.nanoTime();
        AtomicBoolean found = new AtomicBoolean(false);
        AtomicReference<String> match = new AtomicReference<>(null);
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);

        try {
            for (int length = 1; length <= maxLength && !found.get(); length++) {
                long spaceSize = (long) Math.pow(charset.length, length);
                int workers = (int) Math.min(threadCount, Math.max(1, spaceSize));
                long chunk = spaceSize / workers;
                final int len = length;

                List<Callable<Void>> tasks = new ArrayList<>();
                for (int w = 0; w < workers; w++) {
                    final long rangeStart = w * chunk;
                    final long rangeEnd = (w == workers - 1) ? spaceSize : rangeStart + chunk;
                    tasks.add(() -> {
                        for (long index = rangeStart; index < rangeEnd && !found.get(); index++) {
                            String candidate = HashUtil.indexToCandidate(index, charset, len);
                            hashCounter.incrementAndGet();
                            if (HashUtil.sha256(candidate).equals(targetHash)) {
                                if (found.compareAndSet(false, true)) {
                                    match.set(candidate);
                                }
                                break;
                            }
                        }
                        return null;
                    });
                }
                pool.invokeAll(tasks);
            }
        } finally {
            pool.shutdown();
        }

        return new CrackResult(found.get(), match.get(), System.nanoTime() - startTime,
                hashCounter.get(), threadCount);
    }
}
