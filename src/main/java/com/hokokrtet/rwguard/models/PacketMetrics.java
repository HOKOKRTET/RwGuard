package com.hokokrtet.rwguard.models;

import java.util.concurrent.atomic.AtomicLong;

public class PacketMetrics {
    private final AtomicLong totalReceived = new AtomicLong(0);
    private final AtomicLong totalDropped = new AtomicLong(0);
    private final AtomicLong totalViolations = new AtomicLong(0);
    private final AtomicLong totalKicks = new AtomicLong(0);

    private volatile long lastSecondStart = System.currentTimeMillis();
    private final AtomicLong packetsInLastSecond = new AtomicLong(0);
    private volatile long currentPps = 0;

    public void recordPacket() {
        totalReceived.incrementAndGet();
        long now = System.currentTimeMillis();
        if (now - lastSecondStart >= 1000L) {
            synchronized (this) {
                if (now - lastSecondStart >= 1000L) {
                    currentPps = packetsInLastSecond.getAndSet(0);
                    lastSecondStart = now;
                }
            }
        }
        packetsInLastSecond.incrementAndGet();
    }

    public void recordDrop() {
        totalDropped.incrementAndGet();
    }

    public void recordViolation() {
        totalViolations.incrementAndGet();
    }

    public void recordKick() {
        totalKicks.incrementAndGet();
    }

    public long getTotalReceived() {
        return totalReceived.get();
    }

    public long getTotalDropped() {
        return totalDropped.get();
    }

    public long getTotalViolations() {
        return totalViolations.get();
    }

    public long getTotalKicks() {
        return totalKicks.get();
    }

    public long getCurrentPps() {
        return currentPps;
    }

    public void reset() {
        totalReceived.set(0);
        totalDropped.set(0);
        totalViolations.set(0);
        totalKicks.set(0);
        packetsInLastSecond.set(0);
        currentPps = 0;
    }
}
