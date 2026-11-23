package com.hokokrtet.rwguard.checks.ratelimit;

import com.hokokrtet.rwguard.RwGuard;
import com.hokokrtet.rwguard.api.CheckContext;
import com.hokokrtet.rwguard.api.CheckResult;
import com.hokokrtet.rwguard.api.PacketCategory;
import com.hokokrtet.rwguard.checks.AbstractCheck;
import com.hokokrtet.rwguard.models.PacketCounter;
import com.hokokrtet.rwguard.models.PacketLimitRule;
import com.hokokrtet.rwguard.models.PlayerData;

public class PacketRateLimitCheck extends AbstractCheck {
    public PacketRateLimitCheck(RwGuard plugin) {
        super(plugin, "PacketRateLimit", PacketCategory.OTHER);
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public CheckResult process(CheckContext context) {
        String packetName = context.getPacketName();
        PacketLimitRule rule = plugin.getConfigManager().getRule(packetName);
        if (rule == null) {
            return CheckResult.allow();
        }

        PlayerData data = context.getPlayerData();
        PacketCounter counter = data.getCounter(packetName);
        int count = counter.increment(rule.getWindowSeconds());

        if (count > rule.getMaxPerSecond()) {
            return new CheckResult(rule.getAction(), "Packet rate limit exceeded for " + packetName + " (" + count + "/" + rule.getMaxPerSecond() + ")", 1);
        }

        return CheckResult.allow();
    }
}
