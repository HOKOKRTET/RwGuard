package com.hokokrtet.rwguard.checks.chat;

import com.hokokrtet.rwguard.RwGuard;
import com.hokokrtet.rwguard.api.CheckContext;
import com.hokokrtet.rwguard.api.CheckResult;
import com.hokokrtet.rwguard.api.PacketCategory;
import com.hokokrtet.rwguard.checks.AbstractCheck;
import com.hokokrtet.rwguard.models.PacketCounter;
import com.hokokrtet.rwguard.models.PlayerData;

public class ChatFloodCheck extends AbstractCheck {
    public ChatFloodCheck(RwGuard plugin) {
        super(plugin, "ChatFlood", PacketCategory.CHAT);
    }

    @Override
    public boolean isEnabled() {
        return plugin.getConfigManager().getChatMaxPps() > 0;
    }

    @Override
    public CheckResult process(CheckContext context) {
        if (!context.getPacketName().equals("CHAT") && !context.getPacketName().contains("Chat")) {
            return CheckResult.allow();
        }

        PlayerData data = context.getPlayerData();
        PacketCounter counter = data.getCounter("CHAT");
        int count = counter.increment(1);
        int max = plugin.getConfigManager().getChatMaxPps();

        if (count > max) {
            return CheckResult.drop("Chat message spam (" + count + "/" + max + " msg/sec)", 1);
        }

        return CheckResult.allow();
    }
}
