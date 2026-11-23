package com.hokokrtet.rwguard.checks.payload;

import com.hokokrtet.rwguard.RwGuard;
import com.hokokrtet.rwguard.api.CheckContext;
import com.hokokrtet.rwguard.api.CheckResult;
import com.hokokrtet.rwguard.api.PacketCategory;
import com.hokokrtet.rwguard.checks.AbstractCheck;
import com.hokokrtet.rwguard.models.PacketCounter;
import com.hokokrtet.rwguard.models.PlayerData;

public class ChannelRegisterFloodCheck extends AbstractCheck {
    public ChannelRegisterFloodCheck(RwGuard plugin) {
        super(plugin, "ChannelRegisterFlood", PacketCategory.CUSTOM_PAYLOAD);
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public CheckResult process(CheckContext context) {
        if (!context.getPacketName().contains("CUSTOM_PAYLOAD") && !context.getPacketName().contains("CustomPayload")) {
            return CheckResult.allow();
        }

        PlayerData data = context.getPlayerData();
        PacketCounter counter = data.getCounter("CHANNEL_REGISTER");
        int count = counter.increment(1);

        if (count > 25) {
            return CheckResult.drop("Channel register flood (" + count + " per sec)", 3);
        }

        if (data.getRegisteredChannels().size() > plugin.getConfigManager().getPayloadMaxChannels()) {
            return CheckResult.drop("Exceeded maximum registered channels limit (" + data.getRegisteredChannels().size() + ")", 3);
        }

        return CheckResult.allow();
    }
}
