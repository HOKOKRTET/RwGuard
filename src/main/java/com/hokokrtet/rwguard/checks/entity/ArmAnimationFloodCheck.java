package com.hokokrtet.rwguard.checks.entity;

import com.hokokrtet.rwguard.RwGuard;
import com.hokokrtet.rwguard.api.CheckContext;
import com.hokokrtet.rwguard.api.CheckResult;
import com.hokokrtet.rwguard.api.PacketCategory;
import com.hokokrtet.rwguard.checks.AbstractCheck;
import com.hokokrtet.rwguard.models.PacketCounter;
import com.hokokrtet.rwguard.models.PlayerData;

public class ArmAnimationFloodCheck extends AbstractCheck {
    public ArmAnimationFloodCheck(RwGuard plugin) {
        super(plugin, "ArmAnimationFlood", PacketCategory.ENTITY);
    }

    @Override
    public boolean isEnabled() {
        return plugin.getConfigManager().getEntityMaxArmAnimationPps() > 0;
    }

    @Override
    public CheckResult process(CheckContext context) {
        if (!context.getPacketName().contains("ARM_ANIMATION") && !context.getPacketName().contains("Swing")) {
            return CheckResult.allow();
        }

        PlayerData data = context.getPlayerData();
        PacketCounter counter = data.getCounter("ARM_ANIMATION");
        int count = counter.increment(1);
        int max = plugin.getConfigManager().getEntityMaxArmAnimationPps();

        if (count > max) {
            return CheckResult.drop("Arm animation flood (" + count + "/" + max + " per sec)", 1);
        }

        return CheckResult.allow();
    }
}
