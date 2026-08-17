package org.example.pz31.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RateLimiter {

    private final long minIntervalMs;
    private long lastCallAt = 0L;

    public RateLimiter(@Value("${nominatim.min-interval-ms:1000}") long minIntervalMs) {
        this.minIntervalMs = minIntervalMs;
    }


    public synchronized void acquire() {
        long now = System.currentTimeMillis();
        long waitMs = (lastCallAt + minIntervalMs) - now;
        if (waitMs > 0) {
            try {
                Thread.sleep(waitMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Перервано очікування rate limiter", e);
            }
        }
        lastCallAt = System.currentTimeMillis();
    }
}
