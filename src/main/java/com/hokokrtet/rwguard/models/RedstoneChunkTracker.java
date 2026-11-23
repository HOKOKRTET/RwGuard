package com.hokokrtet.rwguard.models;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class RedstoneChunkTracker {
    private static final Map<Long, ChunkActivity> CHUNK_ACTIVITY = new ConcurrentHashMap<>();

    public static long chunkKey(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
    }

    public static int incrementAndGet(int chunkX, int chunkZ, int windowSeconds) {
        long key = chunkKey(chunkX, chunkZ);
        long now = System.currentTimeMillis();
        ChunkActivity activity = CHUNK_ACTIVITY.computeIfAbsent(key, k -> new ChunkActivity(now));
        return activity.increment(now, windowSeconds * 1000L);
    }

    public static void cleanup(long expiryMillis) {
        long now = System.currentTimeMillis();
        CHUNK_ACTIVITY.entrySet().removeIf(entry -> now - entry.getValue().lastUpdate > expiryMillis);
    }

    public static void reset() {
        CHUNK_ACTIVITY.clear();
    }

    private static class ChunkActivity {
        private volatile long windowStart;
        private volatile long lastUpdate;
        private final AtomicInteger count = new AtomicInteger(0);

        public ChunkActivity(long now) {
            this.windowStart = now;
            this.lastUpdate = now;
        }

        public int increment(long now, long windowDuration) {
            this.lastUpdate = now;
            if (now - windowStart > windowDuration) {
                synchronized (this) {
                    if (now - windowStart > windowDuration) {
                        windowStart = now;
                        count.set(0);
                    }
                }
            }
            return count.incrementAndGet();
        }
    }
}
