package com.hokokrtet.rwguard.checks.inventory;

import com.hokokrtet.rwguard.RwGuard;
import com.hokokrtet.rwguard.api.CheckContext;
import com.hokokrtet.rwguard.api.CheckResult;
import com.hokokrtet.rwguard.api.PacketCategory;
import com.hokokrtet.rwguard.checks.AbstractCheck;
import com.hokokrtet.rwguard.models.PacketCounter;
import com.hokokrtet.rwguard.models.PlayerData;

public class WindowClickRateCheck extends AbstractCheck {
    public WindowClickRateCheck(RwGuard plugin) {
        super(plugin, "WindowClickRate", PacketCategory.INVENTORY);
    }

    @Override
    public boolean isEnabled() {
        return plugin.getConfigManager().getInventoryMaxClicksPps() > 0;
    }

    @Override
    public CheckResult process(CheckContext context) {
        if (!context.getPacketName().contains("WINDOW_CLICK") && !context.getPacketName().contains("ContainerClick")) {
            return CheckResult.allow();
        }

        PlayerData data = context.getPlayerData();
        PacketCounter counter = data.getCounter("WINDOW_CLICK");
        int count = counter.increment(1);
        int max = plugin.getConfigManager().getInventoryMaxClicksPps();
        if (data.isBedrock() && plugin.getConfigManager().isBedrockEnabled()) {
            max = (int) (max * plugin.getConfigManager().getBedrockMultiplier());
        }

        if (count > max) {
            return CheckResult.drop("Inventory click flood (" + count + "/" + max + " clicks/sec)", 2);
        }

        return CheckResult.allow();
    }
}
