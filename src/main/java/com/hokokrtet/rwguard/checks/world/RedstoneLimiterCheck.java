package com.hokokrtet.rwguard.checks.world;

import com.hokokrtet.rwguard.RwGuard;
import com.hokokrtet.rwguard.api.CheckContext;
import com.hokokrtet.rwguard.api.CheckResult;
import com.hokokrtet.rwguard.api.PacketCategory;
import com.hokokrtet.rwguard.checks.AbstractCheck;
import com.hokokrtet.rwguard.models.RedstoneChunkTracker;
import org.bukkit.Chunk;

public class RedstoneLimiterCheck extends AbstractCheck {
    public RedstoneLimiterCheck(RwGuard plugin) {
        super(plugin, "RedstoneLimiter", PacketCategory.WORLD);
    }

    @Override
    public boolean isEnabled() {
        return plugin.getConfigManager().isRedstoneLimiterEnabled();
    }

    @Override
    public CheckResult process(CheckContext context) {
        return CheckResult.allow();
    }

    public boolean shouldCancelRedstone(Chunk chunk) {
        if (!isEnabled() || chunk == null) return false;
        int max = plugin.getConfigManager().getRedstoneMaxPerChunk();
        int window = plugin.getConfigManager().getRedstoneWindowSeconds();
        int updates = RedstoneChunkTracker.incrementAndGet(chunk.getX(), chunk.getZ(), window);
        return updates > max;
    }
}
