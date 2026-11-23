package com.hokokrtet.rwguard.checks.inventory;

import com.hokokrtet.rwguard.RwGuard;
import com.hokokrtet.rwguard.api.CheckContext;
import com.hokokrtet.rwguard.api.CheckResult;
import com.hokokrtet.rwguard.api.PacketCategory;
import com.hokokrtet.rwguard.checks.AbstractCheck;
import com.hokokrtet.rwguard.models.PacketCounter;
import com.hokokrtet.rwguard.models.PlayerData;

public class AutoRecipeCrashCheck extends AbstractCheck {
    public AutoRecipeCrashCheck(RwGuard plugin) {
        super(plugin, "AutoRecipeCrash", PacketCategory.INVENTORY);
    }

    @Override
    public boolean isEnabled() {
        return plugin.getConfigManager().isAutoRecipeLimitEnabled();
    }

    @Override
    public CheckResult process(CheckContext context) {
        if (!context.getPacketName().contains("AUTO_RECIPE") && !context.getPacketName().contains("PlaceRecipe")) {
            return CheckResult.allow();
        }

        PlayerData data = context.getPlayerData();
        PacketCounter counter = data.getCounter("AUTO_RECIPE");
        int count = counter.increment(1);
        int max = plugin.getConfigManager().getAutoRecipeMaxPps();

        if (count > max) {
            return CheckResult.drop("Auto-recipe packet flood (" + count + "/" + max + " pps)", 2);
        }

        return CheckResult.allow();
    }
}
