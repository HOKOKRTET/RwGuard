package com.hokokrtet.rwguard.checks.movement;

import com.hokokrtet.rwguard.RwGuard;
import com.hokokrtet.rwguard.api.CheckContext;
import com.hokokrtet.rwguard.api.CheckResult;
import com.hokokrtet.rwguard.api.PacketCategory;
import com.hokokrtet.rwguard.checks.AbstractCheck;
import com.hokokrtet.rwguard.models.PacketCounter;
import com.hokokrtet.rwguard.models.PlayerData;

public class MovementSpamCheck extends AbstractCheck {
    public MovementSpamCheck(RwGuard plugin) {
        super(plugin, "MovementSpam", PacketCategory.MOVEMENT);
    }

    @Override
    public boolean isEnabled() {
        return plugin.getConfigManager().getMovementMaxPps() > 0;
    }

    @Override
    public CheckResult process(CheckContext context) {
        PlayerData data = context.getPlayerData();
        PacketCounter counter = data.getCounter("FLYING_TOTAL");
        int count = counter.increment(1);
        int max = plugin.getConfigManager().getMovementMaxPps();
        if (data.isBedrock() && plugin.getConfigManager().isBedrockEnabled()) {
            max = (int) (max * plugin.getConfigManager().getBedrockMultiplier());
        }

        if (count > max) {
            return CheckResult.drop("Movement packet spam (" + count + "/" + max + " pps)", 1);
        }
        return CheckResult.allow();
    }
}
