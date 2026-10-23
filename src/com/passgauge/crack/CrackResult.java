package com.passgauge.crack;

/** Immutable outcome of a single crack run (sequential or parallel). */
public final class CrackResult {
    private final boolean found;
    private final String matchedPassword;
    private final long elapsedNanos;
    private final long hashesComputed;
    private final int threadsUsed;

    public CrackResult(boolean found, String matchedPassword, long elapsedNanos,
                        long hashesComputed, int threadsUsed) {
        this.found = found;
        this.matchedPassword = matchedPassword;
        this.elapsedNanos = elapsedNanos;
        this.hashesComputed = hashesComputed;
        this.threadsUsed = threadsUsed;
    }

    public boolean isFound() { return found; }
    public String getMatchedPassword() { return matchedPassword; }
    public long getElapsedNanos() { return elapsedNanos; }
    public double getElapsedSeconds() { return elapsedNanos / 1_000_000_000.0; }
    public long getHashesComputed() { return hashesComputed; }
    public int getThreadsUsed() { return threadsUsed; }

    public double getHashesPerSecond() {
        double secs = getElapsedSeconds();
        return secs > 0 ? hashesComputed / secs : hashesComputed;
    }
}
