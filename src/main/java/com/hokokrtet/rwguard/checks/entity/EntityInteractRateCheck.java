package com.hokokrtet.rwguard.checks.entity;

import com.hokokrtet.rwguard.RwGuard;
import com.hokokrtet.rwguard.api.CheckContext;
import com.hokokrtet.rwguard.api.CheckResult;
import com.hokokrtet.rwguard.api.PacketCategory;
import com.hokokrtet.rwguard.checks.AbstractCheck;
import com.hokokrtet.rwguard.models.PacketCounter;
import com.hokokrtet.rwguard.models.PlayerData;

public class EntityInteractRateCheck extends AbstractCheck {
    public EntityInteractRateCheck(RwGuard plugin) {
        super(plugin, "EntityInteractRate", PacketCategory.ENTITY);
    }

    @Override
    public boolean isEnabled() {
        return plugin.getConfigManager().getEntityMaxInteractsPps() > 0;
    }

    @Override
    public CheckResult process(CheckContext context) {
        if (!context.getPacketName().contains("USE_ENTITY") && !context.getPacketName().contains("Interact")) {
            return CheckResult.allow();
        }

        PlayerData data = context.getPlayerData();
        PacketCounter counter = data.getCounter("USE_ENTITY");
        int count = counter.increment(1);
        int max = plugin.getConfigManager().getEntityMaxInteractsPps();

        if (count > max) {
            return CheckResult.drop("Entity interaction spam (" + count + "/" + max + " per sec)", 2);
        }

        return CheckResult.allow();
    }
}
