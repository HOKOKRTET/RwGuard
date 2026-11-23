package com.hokokrtet.rwguard.models;

import java.util.concurrent.atomic.AtomicInteger;

public class PacketCounter {
    private final AtomicInteger count = new AtomicInteger(0);
    private volatile long lastReset = System.currentTimeMillis();

    public int incrementAndGet() {
        return count.incrementAndGet();
    }

    public int increment(int windowSeconds) {
        long now = System.currentTimeMillis();
        long windowMillis = Math.max(1, windowSeconds) * 1000L;
        if (now - lastReset > windowMillis) {
            synchronized (this) {
                if (now - lastReset > windowMillis) {
                    count.set(0);
                    lastReset = now;
                }
            }
        }
        return count.incrementAndGet();
    }

    public int get() {
        return count.get();
    }

    public void reset() {
        count.set(0);
        lastReset = System.currentTimeMillis();
    }

    public long getLastReset() {
        return lastReset;
    }
}
