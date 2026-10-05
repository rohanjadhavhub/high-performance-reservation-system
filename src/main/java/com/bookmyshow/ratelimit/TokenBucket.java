package com.bookmyshow.ratelimit;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Token Bucket rate limiter for admission control.
 * Protects the Redis locking mechanism from thundering herds
 * during blockbuster movie releases.
 */
public class TokenBucket {

    private final long capacity;
    private final long refillTokens;
    private final long refillIntervalMs;
    private final AtomicLong tokens;
    private volatile long lastRefillTimestamp;

    public TokenBucket(long capacity, long refillTokens, long refillIntervalMs) {
        this.capacity = capacity;
        this.refillTokens = refillTokens;
        this.refillIntervalMs = refillIntervalMs;
        this.tokens = new AtomicLong(capacity);
        this.lastRefillTimestamp = System.currentTimeMillis();
    }

    /**
     * Try to consume one token.
     * @return true if a token was available, false if rate limited.
     */
    public synchronized boolean tryConsume() {
        refill();
        if (tokens.get() > 0) {
            tokens.decrementAndGet();
            return true;
        }
        return false;
    }

    /**
     * Try to consume N tokens.
     * @return true if enough tokens were available.
     */
    public synchronized boolean tryConsume(long numTokens) {
        refill();
        if (tokens.get() >= numTokens) {
            tokens.addAndGet(-numTokens);
            return true;
        }
        return false;
    }

    private void refill() {
        long now = System.currentTimeMillis();
        long elapsed = now - lastRefillTimestamp;
        if (elapsed >= refillIntervalMs) {
            long intervals = elapsed / refillIntervalMs;
            long tokensToAdd = intervals * refillTokens;
            long newTokens = Math.min(capacity, tokens.get() + tokensToAdd);
            tokens.set(newTokens);
            lastRefillTimestamp = now;
        }
    }

    public long getAvailableTokens() {
        refill();
        return tokens.get();
    }
}
