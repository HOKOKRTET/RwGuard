package com.hokokrtet.rwguard.checks.ratelimit;

import com.hokokrtet.rwguard.RwGuard;
import com.hokokrtet.rwguard.api.CheckContext;
import com.hokokrtet.rwguard.api.CheckResult;
import com.hokokrtet.rwguard.api.PacketCategory;
import com.hokokrtet.rwguard.checks.AbstractCheck;
import com.hokokrtet.rwguard.models.PacketCounter;
import com.hokokrtet.rwguard.models.PlayerData;

public class GlobalRateLimitCheck extends AbstractCheck {
    public GlobalRateLimitCheck(RwGuard plugin) {
        super(plugin, "GlobalRateLimit", PacketCategory.OTHER);
    }

    @Override
    public boolean isEnabled() {
        return plugin.getConfigManager().getGlobalPacketLimit() > 0;
    }

    @Override
    public CheckResult process(CheckContext context) {
        PlayerData data = context.getPlayerData();
        PacketCounter counter = data.getTotalPacketCounter();
        int count = counter.increment(1);
        int max = plugin.getConfigManager().getGlobalPacketLimit();
        if (data.isBedrock() && plugin.getConfigManager().isBedrockEnabled()) {
            max = (int) (max * plugin.getConfigManager().getBedrockMultiplier());
        }

        if (count > max) {
            return CheckResult.drop("Global packet rate limit exceeded (" + count + "/" + max + " pps)", 2);
        }
        return CheckResult.allow();
    }
}
