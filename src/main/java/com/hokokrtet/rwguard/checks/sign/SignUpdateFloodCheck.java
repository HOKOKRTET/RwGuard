package com.hokokrtet.rwguard.checks.sign;

import com.hokokrtet.rwguard.RwGuard;
import com.hokokrtet.rwguard.api.CheckContext;
import com.hokokrtet.rwguard.api.CheckResult;
import com.hokokrtet.rwguard.api.PacketCategory;
import com.hokokrtet.rwguard.checks.AbstractCheck;
import com.hokokrtet.rwguard.models.PacketCounter;
import com.hokokrtet.rwguard.models.PlayerData;

public class SignUpdateFloodCheck extends AbstractCheck {
    public SignUpdateFloodCheck(RwGuard plugin) {
        super(plugin, "SignUpdateFlood", PacketCategory.SIGN);
    }

    @Override
    public boolean isEnabled() {
        return plugin.getConfigManager().getSignMaxUpdatesPps() > 0;
    }

    @Override
    public CheckResult process(CheckContext context) {
        if (!context.getPacketName().contains("UPDATE_SIGN") && !context.getPacketName().contains("SignUpdate")) {
            return CheckResult.allow();
        }

        PlayerData data = context.getPlayerData();
        PacketCounter counter = data.getCounter("SIGN_UPDATE");
        int count = counter.increment(1);
        int max = plugin.getConfigManager().getSignMaxUpdatesPps();

        if (count > max) {
            return CheckResult.drop("Sign update spam (" + count + "/" + max + " per sec)", 2);
        }

        return CheckResult.allow();
    }
}
